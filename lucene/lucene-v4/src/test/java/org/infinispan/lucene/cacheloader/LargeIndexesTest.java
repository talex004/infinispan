package org.infinispan.lucene.cacheloader;

import java.io.IOException;

import org.apache.lucene.store.IndexInput;
import org.infinispan.persistence.spi.PersistenceException;
import org.infinispan.lucene.ChunkCacheKey;
import org.infinispan.lucene.FileCacheKey;
import org.infinispan.lucene.FileMetadata;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * Test for extra-large indexes, where an int isn't large enough to hold file sizes.
 * 
 * @author Sanne Grinovero
 * @since 5.2
 */
@Test(groups = "functional", testName = "lucene.cachestore.LargeIndexesTest")
public class LargeIndexesTest {

   private static final String INDEX_NAME = "myIndex";
   private static final String FILE_NAME = "largeFile";
   private static final long TEST_SIZE = ((long)Integer.MAX_VALUE) + 10;//something not fitting in int
   private static final int AUTO_BUFFER = 16;//ridiculously low

   public void testAutoChunkingOnLargeFiles() throws PersistenceException {
      FileCacheKey k = new FileCacheKey(INDEX_NAME, FILE_NAME);
      DirectoryLoaderAdaptor adaptor = new DirectoryLoaderAdaptor(new InternalDirectoryContractImpl(), INDEX_NAME, AUTO_BUFFER);
      Object loaded = adaptor.load(k);
      AssertJUnit.assertTrue(loaded instanceof FileMetadata);
      FileMetadata metadata = (FileMetadata)loaded;
      AssertJUnit.assertEquals(23, metadata.getLastModified());
      AssertJUnit.assertEquals(TEST_SIZE, metadata.getSize());
      AssertJUnit.assertEquals(AUTO_BUFFER, metadata.getBufferSize());
   }

   public void testSmallChunkLoading() throws PersistenceException {
      DirectoryLoaderAdaptor adaptor = new DirectoryLoaderAdaptor(new InternalDirectoryContractImpl(), INDEX_NAME, AUTO_BUFFER);
      Object loaded = adaptor.load(new ChunkCacheKey(INDEX_NAME, FILE_NAME, 0, AUTO_BUFFER));
      AssertJUnit.assertTrue(loaded instanceof byte[]);
      AssertJUnit.assertEquals(AUTO_BUFFER, ((byte[])loaded).length);
      loaded = adaptor.load(new ChunkCacheKey(INDEX_NAME, FILE_NAME, 5, AUTO_BUFFER));
      AssertJUnit.assertTrue(loaded instanceof byte[]);
      AssertJUnit.assertEquals(AUTO_BUFFER, ((byte[])loaded).length);
      final int lastChunk = (int)(TEST_SIZE / AUTO_BUFFER);
      final long lastChunkSize = TEST_SIZE % AUTO_BUFFER;
      AssertJUnit.assertEquals(9, lastChunkSize);
      loaded = adaptor.load(new ChunkCacheKey(INDEX_NAME, FILE_NAME, lastChunk, AUTO_BUFFER));
      AssertJUnit.assertTrue(loaded instanceof byte[]);
      AssertJUnit.assertEquals(lastChunkSize, ((byte[])loaded).length);
   }

   //Simple home-made mock:
   private static class InternalDirectoryContractImpl implements InternalDirectoryContract {

      @Override
      public String[] listAll() throws IOException {
         AssertJUnit.fail("should not be invoked");
         return null;//compiler thinks we could reach this
      }

      @Override
      public long fileLength(String fileName) throws IOException {
         AssertJUnit.assertEquals(FILE_NAME, fileName);
         return TEST_SIZE;
      }

      @Override
      public void close() throws IOException {
      }

      @Override
      public long fileModified(String fileName) {
         AssertJUnit.assertEquals(FILE_NAME, fileName);
         return 23;
      }

      @Override
      public IndexInput openInput(String fileName) {
         AssertJUnit.assertEquals(FILE_NAME, fileName);
         return new IndexInputMock(fileName);
      }

      @Override
      public boolean fileExists(String fileName) {
         AssertJUnit.fail("should not be invoked");
         return false;
      }
   }

   private static class IndexInputMock extends IndexInput {

      protected IndexInputMock(String resourceDescription) {
         super(resourceDescription);
      }

      private boolean closed = false;
      private long position = 0;

      @Override
      public void close() throws IOException {
         AssertJUnit.assertFalse(closed);
         closed = true;
      }

      @Override
      public long getFilePointer() {
         AssertJUnit.fail("should not be invoked");
         return 0;
      }

      @Override
      public void seek(long pos) throws IOException {
         position = pos;
      }

      @Override
      public long length() {
         return TEST_SIZE;
      }

      @Override
      public byte readByte() throws IOException {
         return 0;
      }

      @Override
      public void readBytes(byte[] b, int offset, int len) throws IOException {
         final long remainingFileSize = TEST_SIZE - position;
         final long expectedReadSize = Math.min(remainingFileSize, AUTO_BUFFER);
         AssertJUnit.assertEquals(expectedReadSize, b.length);
         AssertJUnit.assertEquals(0, offset);
         AssertJUnit.assertEquals(expectedReadSize, len);
      }
   }

}
