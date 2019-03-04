package io.takari.jdkget.win;

import static io.takari.jdkget.win.CabConstants.*;
import static io.takari.jdkget.win.CabIO.*;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.io.output.ProxyOutputStream;

import io.takari.jdkget.win.Cabinet.CabFile;
import io.takari.jdkget.win.Cabinet.CabFolder;
import io.takari.jdkget.win.Cabinet.CabFolderData;

public class CabDecompressor implements AutoCloseable {

  private DecompressorState d;
  private int[] params = new int[3];

  public static class DecompressorState {
    CabFolder folder; /* current folder we're extracting from */

    CabFolderData data; /* current folder split we're in */

    long offset; /* uncompressed offset within folder */

    int block; /* which block are we decompressing? */

    int comp_type; /* type of compression used by folder */

    DecompressionMethod decompressor; /* decompressor code */

    // void *state; /* decompressor state */

    Cabinet incab; /* cabinet where input data comes from */

    CabInput in; /* input file handle */

    OutputStream out; /* output file handle */

    byte[] i; /* input data consumed, end */
    int i_ptr, i_end;

    byte[] input = new byte[CAB_INPUTMAX]; /* one input block of data */
  }

  static interface DecompressionMethod {

    void free();

    void decompress(ByteSource input, OutputStream output, long out_bytes) throws IOException;

  }

  class Extractor {

    private final long fileLength;

    public Extractor(long fileLength) {
      this.fileLength = fileLength;
    }

    public void extract(OutputStream out) throws IOException {
      extract(out, fileLength);
    }

    public void extract(OutputStream out, long length) throws IOException {
      ByteSource bs = new ByteSource() {
        @Override
        public int read(byte[] buf, int offset, int len) throws IOException {
          return cabd_sys_read(buf, offset, len);
        }
      };

      out = new ProxyOutputStream(out) {
        @Override
        protected void afterWrite(int n) throws IOException {
          d.offset += n;
        }
      };

      long avail = available();
      if (length > avail) {
        length = avail;
      }

      d.decompressor.decompress(bs, out, length);
    }
    
    public long available() {
      return fileLength - d.offset;
    }

  }

  public CabDecompressor() {
    setParam(MSCABD_PARAM_FIXMSZIP, 0);
    setParam(MSCABD_PARAM_DECOMPBUF, 4096);
  }

  public void setParam(int param, int value) {
    params[param] = value;
  }

  @Override
  public void close() {
    cabd_free_decomp();
  }

  /***************************************
   * CABD_EXTRACT
   ***************************************
   * extracts a file from a cabinet
   */
  Extractor cabd_extract(Cabinet cab, CabFile file) throws IOException {
    CabFolder fol = file.folder;

    /* validate the file's offset and length */
    if ((file.offset > CAB_LENGTHMAX) || (file.length > CAB_LENGTHMAX) || ((file.offset + file.length) > CAB_LENGTHMAX)) {
      throw new IOException("invalid file offset and/or length");
    }

    /* check if file can be extracted */
    if (fol == null || fol.merge_prev != null || (((file.offset + file.length) / CAB_BLOCKMAX) > fol.num_blocks)) {
      throw new IOException(String.format("file \"%s\" cannot be extracted, cabinet set is incomplete.", file.filename));
    }

    /* allocate generic decompression state */
    if (d == null) {
      d = new DecompressorState();
    }

    /* do we need to change folder or reset the current folder? */
    if ((d.folder != fol) || (d.offset > file.offset) || d.decompressor == null) {
      /* free any existing decompressor */
      cabd_free_decomp();

      /* do we need to open a new cab file? */
      if (d.in == null || (fol.data.cab != d.incab)) {
        d.incab = fol.data.cab;
        d.in = cab.in;
      }

      /* seek to start of data blocks */
      d.in.seekAbsolute(fol.data.offset);

      /* set up decompressor */
      cabd_init_decomp(fol);

      /* initialise new folder state */
      d.folder = fol;
      d.data = fol.data;
      d.offset = 0;
      d.block = 0;
      d.i = d.input;
      d.i_ptr = d.i_end = 0;
    }

    /* if file has more than 0 bytes */
    if (file.length > 0) {
      long bytes;
      /*
       * get to correct offset. - if cabd_sys_read() has an error, it will set self->read_error and pass back MSPACK_ERR_READ
       */
      if ((bytes = file.offset - d.offset) > 0) {
        d.decompressor.decompress(d.in, NullOutputStream.NULL_OUTPUT_STREAM, bytes);
      }

      /* if getting to the correct offset was error free, unpack file */
      return new Extractor(file.length);
    }
    return null;
  }

  /***************************************
   * CABD_INIT_DECOMP
   ***************************************
   * cabd_init_decomp initialises decompression state, according to which decompression method was used. relies on
   * self->d->folder being the same as when initialised.
   */
  void cabd_init_decomp(CabFolder fol) throws IOException {
    d.comp_type = fol.getCompMethod();

    switch (d.comp_type) {
      case cffoldCOMPTYPE_LZX:
        LzxDecompressionMethod lzx = new LzxDecompressionMethod();
        lzx.init(fol.getCompLevel(), 0, params[MSCABD_PARAM_DECOMPBUF], 0L, false);
        d.decompressor = lzx;
        break;
      
       case cffoldCOMPTYPE_MSZIP:
         //self->d->decompress = (int (*)(void *, off_t)) &mszipd_decompress;
         //self->d->state = mszipd_init(&self->d->sys, fh, fh, self->param[MSCABD_PARAM_DECOMPBUF], self->param[MSCABD_PARAM_FIXMSZIP]); 
         //break; 
       case cffoldCOMPTYPE_NONE: 
         //self->d->decompress = (int (*)(void *, off_t)) &noned_decompress;
         //self->d->state = noned_init(&self->d->sys, fh, fh, self->param[MSCABD_PARAM_DECOMPBUF]) 
         //break; 
       case cffoldCOMPTYPE_QUANTUM: 
         //self->d->decompress = (int (*)(void *, off_t)) &qtmd_decompress; 
         //self->d->state = qtmd_init(&self->d->sys, fh, fh, (int) (ct >> 8) & 0x1f, self->param[MSCABD_PARAM_DECOMPBUF]); 
         //break;
       
      default:
        throw new IOException("unsupported compression format: " + d.comp_type);
    }
  }

  /***************************************
   * CABD_FREE_DECOMP
   ***************************************
   * cabd_free_decomp frees decompression state, according to which method was used.
   */
  void cabd_free_decomp() {
    if (d == null || d.decompressor == null)
      return;
    d.decompressor.free();
    d.decompressor = null;
  }


  /***************************************
   * CABD_SYS_READ, CABD_SYS_WRITE
   ***************************************
   * cabd_sys_read is the internal reader function which the decompressors use. will read data blocks (and merge split
   * blocks) from the cabinet and serve the read bytes to the decompressors
   *
   * cabd_sys_write is the internal writer function which the decompressors use. it either writes data to disk
   * (self->d->outfh) with the real
   */
  int cabd_sys_read(byte[] buf, int offset, int bytes) throws IOException {
    int avail, todo, outlen;

    boolean ignore_cksum = params[MSCABD_PARAM_FIXMSZIP] != 0 && (d.comp_type == cffoldCOMPTYPE_MSZIP);

    todo = bytes;
    while (todo > 0) {
      avail = d.i_end - d.i_ptr;

      /* if out of input data, read a new block */
      if (avail > 0) {
        /* copy as many input bytes available as possible */
        if (avail > todo)
          avail = todo;
        System.arraycopy(d.i, d.i_ptr, buf, offset, avail);
        d.i_ptr += avail;
        offset += avail;
        todo -= avail;
      } else {
        /* out of data, read a new block */

        /* check if we're out of input blocks, advance block counter */
        if (d.block++ >= d.folder.num_blocks) {
          break;
        }

        /* read a block */
        outlen = cabd_sys_read_block(ignore_cksum);

        /*
         * special Quantum hack -- trailer byte to allow the decompressor to realign itself. CAB Quantum blocks, unlike LZX
         * blocks, can have anything from 0 to 4 trailing null bytes.
         */
        if (d.comp_type == cffoldCOMPTYPE_QUANTUM) {
          d.i[d.i_end++] = (byte) 0xFF;
        }

        /* is this the last block? */
        if (d.block >= d.folder.num_blocks) {
          /* last block */
          if (d.comp_type == cffoldCOMPTYPE_LZX) {
            /*
             * special LZX hack -- on the last block, inform LZX of the size of the output data stream.
             */
            ((LzxDecompressionMethod) d.decompressor).setOutputLength(((d.block - 1) * CAB_BLOCKMAX) + outlen);
          }
        } else {
          /* not the last block */
          if (outlen != CAB_BLOCKMAX) {
            // warn( "WARNING; non-maximal data block");
          }
        }
      } /* if (avail) */
    } /* while (todo > 0) */
    return bytes - todo;
  }

  /***************************************
   * CABD_SYS_READ_BLOCK
   ***************************************
   * reads a whole data block from a cab file. the block may span more than one cab file, if it does then the fragments
   * will be reassembled
   */
  int cabd_sys_read_block(boolean ignore_cksum) throws IOException {
    byte[] hdr = new byte[cfdata_SIZEOF];
    int cksum;
    int len;

    /* reset the input block pointer and end of block pointer */
    d.i = d.input;
    d.i_ptr = d.i_end = 0;

    do {
      /* read the block header */
      if (d.in.read(hdr, 0, cfdata_SIZEOF) != cfdata_SIZEOF) {
        throw new IOException("error reading cfdata header");
      }

      /* skip any reserved block headers */
      if (d.data.cab.block_resv > 0) {
        d.in.seekRelative(d.data.cab.block_resv);
      }

      /* blocks must not be over CAB_INPUTMAX in size */
      len = EndGetI16(hdr, cfdata_CompressedSize);
      if (((d.i_end - d.i_ptr) + len) > CAB_INPUTMAX) {
        throw new IOException(String.format("block size > CAB_INPUTMAX (%ld + %d)", (long) (d.i_end - d.i_ptr), len));
      }

      /* blocks must not expand to more than CAB_BLOCKMAX */
      if (EndGetI16(hdr, cfdata_UncompressedSize) > CAB_BLOCKMAX) {
        throw new IOException("block size > CAB_BLOCKMAX");
      }

      /* read the block data */
      if (d.in.read(d.i, d.i_end, len) != len) {
        throw new IOException("Error reading data block");
      }

      /* perform checksum test on the block (if one is stored) */
      if ((cksum = EndGetI32(hdr, cfdata_CheckSum)) != 0) {
        int sum2 = cabd_checksum(d.i, d.i_end, len, 0);
        if (cabd_checksum(hdr, 4, 4, sum2) != cksum) {
          if (!ignore_cksum) {
            throw new IOException();
          }
          // warn(d->infh, "WARNING; bad block checksum found");
        }
      }

      /* advance end of block pointer to include newly read data */
      d.i_end += len;

      /*
       * uncompressed size == 0 means this block was part of a split block and it continues as the first block of the next
       * cabinet in the set. otherwise, this is the last part of the block, and no more block reading needs to be done.
       */
      /* EXIT POINT OF LOOP -- uncompressed size != 0 */
      int out;
      if ((out = EndGetI16(hdr, cfdata_UncompressedSize)) != 0) {
        return out;
      }

      /* otherwise, advance to next cabinet */

      /* advance to next member in the cabinet set */
      if ((d.data = d.data.next) == null) {
        throw new IOException("ran out of splits in cabinet set");
      }

      /* open next cab file */
      d.incab = d.data.cab;
      d.in = d.incab.in;

      /* seek to start of data blocks */
      d.in.seekAbsolute(d.data.offset);

    } while (true);
  }

  static int cabd_checksum(byte[] data, int off, int bytes, int cksum) {
    int len, ul = 0;

    for (len = bytes >>> 2; len-- > 0; off += 4) {
      cksum ^= ((data[off] & 0xFF) | ((data[off + 1] & 0xFF) << 8) | ((data[off + 2] & 0xFF) << 16) | ((data[off + 3] & 0xFF) << 24));
    }

    switch (bytes & 3) {
      case 3:
        ul |= (data[off++] & 0xFF) << 16;
      case 2:
        ul |= (data[off++] & 0xFF) << 8;
      case 1:
        ul |= (data[off] & 0xFF);
    }
    cksum ^= ul;
    return cksum;
  }
}
