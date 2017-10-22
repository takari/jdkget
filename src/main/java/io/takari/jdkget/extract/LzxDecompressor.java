package io.takari.jdkget.extract;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// https://github.com/kyz/libmspack/blob/6139a0b9e93fcb7fcf423e56aa825bc869e02229/libmspack/mspack/lzxd.c

public class LzxDecompressor {

  private static final int LZX_MIN_MATCH = 2;
  private static final int LZX_MAX_MATCH = 257;
  private static final int LZX_NUM_CHARS = 256;
  private static final int LZX_BLOCKTYPE_INVALID = 0; /* also blocktypes 4-7 invalid */
  private static final int LZX_BLOCKTYPE_VERBATIM = 1;
  private static final int LZX_BLOCKTYPE_ALIGNED = 2;
  private static final int LZX_BLOCKTYPE_UNCOMPRESSED = 3;
  private static final int LZX_PRETREE_NUM_ELEMENTS = 20;
  private static final int LZX_ALIGNED_NUM_ELEMENTS = 8; /* aligned offset tree #elements */
  private static final int LZX_NUM_PRIMARY_LENGTHS = 7; /* this one missing from spec! */
  private static final int LZX_NUM_SECONDARY_LENGTHS = 249; /* length tree #elements */

  /* LZX huffman defines: tweak tablebits as desired */
  private static final int LZX_PRETREE_MAXSYMBOLS = LZX_PRETREE_NUM_ELEMENTS;
  private static final int LZX_PRETREE_TABLEBITS = 6;
  private static final int LZX_MAINTREE_MAXSYMBOLS = LZX_NUM_CHARS + 290 * 8;
  private static final int LZX_MAINTREE_TABLEBITS = 12;
  private static final int LZX_LENGTH_MAXSYMBOLS = LZX_NUM_SECONDARY_LENGTHS + 1;
  private static final int LZX_LENGTH_TABLEBITS = 12;
  private static final int LZX_ALIGNED_MAXSYMBOLS = LZX_ALIGNED_NUM_ELEMENTS;
  private static final int LZX_ALIGNED_TABLEBITS = 7;
  private static final int LZX_LENTABLE_SAFETY = 64; /* table decoding overruns are allowed */

  private static final int LZX_FRAME_SIZE = 32768; /* the size of a frame in LZX */

  private static final int HUFF_MAXBITS = 16;

  private static final int BITBUF_WIDTH = 32;

  /*
   * LZX static data tables:
   *
   * LZX uses 'position slots' to represent match offsets. For every match, a small 'position slot' number and a small
   * offset from that slot are encoded instead of one large offset.
   *
   * The number of slots is decided by how many are needed to encode the largest offset for a given window size. This is
   * easy when the gap between slots is less than 128Kb, it's a linear relationship. But when extra_bits reaches its limit
   * of 17 (because LZX can only ensure reading 17 bits of data at a time), we can only jump 128Kb at a time and have to
   * start using more and more position slots as each window size doubles.
   *
   * position_base[] is an index to the position slot bases
   *
   * extra_bits[] states how many bits of offset-from-base data is needed.
   *
   * They are calculated as follows: extra_bits[i] = 0 where i < 4 extra_bits[i] = floor(i/2)-1 where i >= 4 && i < 36
   * extra_bits[i] = 17 where i >= 36 position_base[0] = 0 position_base[i] = position_base[i-1] + (1 << extra_bits[i-1])
   */
  private static final int[] position_slots = {
      30, 32, 34, 36, 38, 42, 50, 66, 98, 162, 290
  };
  private static final int[] extra_bits = {
      0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8,
      9, 9, 10, 10, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15, 16, 16
  };
  private static final int[] position_base = {
      0, 1, 2, 3, 4, 6, 8, 12, 16, 24, 32, 48, 64, 96, 128, 192, 256, 384, 512,
      768, 1024, 1536, 2048, 3072, 4096, 6144, 8192, 12288, 16384, 24576, 32768,
      49152, 65536, 98304, 131072, 196608, 262144, 393216, 524288, 655360,
      786432, 917504, 1048576, 1179648, 1310720, 1441792, 1572864, 1703936,
      1835008, 1966080, 2097152, 2228224, 2359296, 2490368, 2621440, 2752512,
      2883584, 3014656, 3145728, 3276800, 3407872, 3538944, 3670016, 3801088,
      3932160, 4063232, 4194304, 4325376, 4456448, 4587520, 4718592, 4849664,
      4980736, 5111808, 5242880, 5373952, 5505024, 5636096, 5767168, 5898240,
      6029312, 6160384, 6291456, 6422528, 6553600, 6684672, 6815744, 6946816,
      7077888, 7208960, 7340032, 7471104, 7602176, 7733248, 7864320, 7995392,
      8126464, 8257536, 8388608, 8519680, 8650752, 8781824, 8912896, 9043968,
      9175040, 9306112, 9437184, 9568256, 9699328, 9830400, 9961472, 10092544,
      10223616, 10354688, 10485760, 10616832, 10747904, 10878976, 11010048,
      11141120, 11272192, 11403264, 11534336, 11665408, 11796480, 11927552,
      12058624, 12189696, 12320768, 12451840, 12582912, 12713984, 12845056,
      12976128, 13107200, 13238272, 13369344, 13500416, 13631488, 13762560,
      13893632, 14024704, 14155776, 14286848, 14417920, 14548992, 14680064,
      14811136, 14942208, 15073280, 15204352, 15335424, 15466496, 15597568,
      15728640, 15859712, 15990784, 16121856, 16252928, 16384000, 16515072,
      16646144, 16777216, 16908288, 17039360, 17170432, 17301504, 17432576,
      17563648, 17694720, 17825792, 17956864, 18087936, 18219008, 18350080,
      18481152, 18612224, 18743296, 18874368, 19005440, 19136512, 19267584,
      19398656, 19529728, 19660800, 19791872, 19922944, 20054016, 20185088,
      20316160, 20447232, 20578304, 20709376, 20840448, 20971520, 21102592,
      21233664, 21364736, 21495808, 21626880, 21757952, 21889024, 22020096,
      22151168, 22282240, 22413312, 22544384, 22675456, 22806528, 22937600,
      23068672, 23199744, 23330816, 23461888, 23592960, 23724032, 23855104,
      23986176, 24117248, 24248320, 24379392, 24510464, 24641536, 24772608,
      24903680, 25034752, 25165824, 25296896, 25427968, 25559040, 25690112,
      25821184, 25952256, 26083328, 26214400, 26345472, 26476544, 26607616,
      26738688, 26869760, 27000832, 27131904, 27262976, 27394048, 27525120,
      27656192, 27787264, 27918336, 28049408, 28180480, 28311552, 28442624,
      28573696, 28704768, 28835840, 28966912, 29097984, 29229056, 29360128,
      29491200, 29622272, 29753344, 29884416, 30015488, 30146560, 30277632,
      30408704, 30539776, 30670848, 30801920, 30932992, 31064064, 31195136,
      31326208, 31457280, 31588352, 31719424, 31850496, 31981568, 32112640,
      32243712, 32374784, 32505856, 32636928, 32768000, 32899072, 33030144,
      33161216, 33292288, 33423360
  };

  private InputStream input;

  private long offset; /* number of bytes actually output */
  private long length; /* overall decompressed length of stream */

  private byte[] window; /* decoding window */
  private int window_size; /* window size */
  private int ref_data_size; /* LZX DELTA reference data size */
  private int num_offsets; /* number of match_offset entries in table */
  private int window_posn; /* decompression offset within window */

  private int frame_posn; /* current frame offset within in window */
  private int frame; /* the number of 32kb frames processed */
  private int reset_interval; /* which frame do we reset the compressor? */

  private int R0, R1, R2; /* for the LRU offset system */
  private int block_length; /* uncompressed length of this LZX block */
  private int block_remaining; /* uncompressed bytes still left to decode */

  private int block_type; /* type of the current block */
  private boolean header_read; /* have we started decoding at all yet? */
  private boolean input_end; /* have we reached the end of input? */
  private boolean is_delta; /* does stream follow LZX DELTA spec? */

  /* I/O buffering */
  private byte[] inbuf;

  /* huffman decoding tables */
  private HuffTable preTree;
  private HuffTable mainTree;
  private HuffTable lengthTree;
  private HuffTable alignedTree;

  private int intel_filesize; /* magic header value used for transform */
  private int intel_curpos; /* current offset in transform space */
  private boolean intel_started; /* has intel E8 decoding started? */
  private final byte[] e8_buf = new byte[LZX_FRAME_SIZE];

  private byte[] o;
  private int o_off, o_end;
  private int i_off, i_end;

  private int bit_buffer;
  private int bits_left;

  public void resetState() {
    int i;

    R0 = 1;
    R1 = 1;
    R2 = 1;
    header_read = false;
    block_remaining = 0;
    block_type = LZX_BLOCKTYPE_INVALID;

    /* initialise tables to 0 (because deltas will be applied to them) */
    for (i = 0; i < LZX_MAINTREE_MAXSYMBOLS; i++)
      mainTree.len[i] = 0;
    for (i = 0; i < LZX_LENGTH_MAXSYMBOLS; i++)
      lengthTree.len[i] = 0;
  }

  public void init(int window_bits, int reset_interval, int input_buffer_size, long output_length, boolean is_delta) {

    int window_size = 1 << window_bits;

    /*
     * LZX DELTA window sizes are between 2^17 (128KiB) and 2^25 (32MiB), regular LZX windows are between 2^15 (32KiB) and
     * 2^21 (2MiB)
     */
    if (is_delta) {
      if (window_bits < 17 || window_bits > 25)
        throw new IllegalArgumentException("window_bits");
    } else {
      if (window_bits < 15 || window_bits > 21)
        throw new IllegalArgumentException("window_bits");
    }

    if (reset_interval < 0 || output_length < 0) {
      throw new IllegalArgumentException("reset interval or output length < 0");
    }

    /* round up input buffer size to multiple of two */
    input_buffer_size = (input_buffer_size + 1) & -2;
    if (input_buffer_size < 2)
      throw new IllegalArgumentException("input_buffer_size");

    /* allocate decompression window and input buffer */
    this.window_size = window_size;
    this.window = new byte[window_size];
    this.inbuf = new byte[input_buffer_size];

    /* initialise decompression state */
    this.offset = 0;
    this.length = output_length;

    this.ref_data_size = 0;
    this.window_posn = 0;
    this.frame_posn = 0;
    this.frame = 0;
    this.reset_interval = reset_interval;
    this.intel_filesize = 0;
    this.intel_curpos = 0;
    this.intel_started = false;
    this.num_offsets = position_slots[window_bits - 15] << 3;
    this.is_delta = is_delta;

    this.o = this.e8_buf;
    this.o_off = this.o_end = 0;

    this.preTree = new HuffTable("pretree", LZX_PRETREE_MAXSYMBOLS, LZX_PRETREE_TABLEBITS);
    this.mainTree = new HuffTable("maintree", LZX_MAINTREE_MAXSYMBOLS, LZX_MAINTREE_TABLEBITS);
    this.lengthTree = new HuffTable("length", LZX_LENGTH_MAXSYMBOLS, LZX_LENGTH_TABLEBITS);
    this.alignedTree = new HuffTable("aligned", LZX_ALIGNED_MAXSYMBOLS, LZX_ALIGNED_TABLEBITS);

    resetState();

    this.i_off = this.i_end = 0;
    this.bit_buffer = 0;
    this.bits_left = 0;
    this.input_end = false;
  }

  public void setReferenceData(InputStream in, int length) throws IOException {

    if (!is_delta) {
      throw new IllegalStateException("only LZX DELTA streams support reference data");
    }
    if (offset > 0) {
      throw new IllegalStateException("too late to set reference data after decoding starts");
    }
    if (length > window_size) {
      throw new IllegalStateException(String.format("reference length (%u) is longer than the window", length));
    }
    if (length > 0 && in == null) {
      throw new IllegalStateException("length > 0 but no system or input");
    }

    if (length > 0) {
      /* copy reference data */
      int bytes = in.read(window, window_size - length, length);
      /* length can't be more than 2^25, so no signedness problem */
      if (bytes < (int) length)
        throw new IOException("Unexpected end of stream");
    }
    ref_data_size = length;
  }

  public void setOutputLength(long outBytes) {
    if (outBytes > 0) {
      this.length = outBytes;
    }
  }

  public long getOutputLength() {
    return length;
  }

  public void decompress(InputStream input, OutputStream output, long out_bytes) throws IOException {
    this.input = input;

    /* huffman reading variables */

    int match_length, length_footer, extra, verbatim_bits, bytes_todo;
    int this_run, main_element, aligned_bits, j, rundest, runsrc;
    byte[] buf = new byte[12];
    int frame_size = 0, end_frame, match_offset;

    /* easy answers */
    if (out_bytes < 0)
      throw new IllegalArgumentException();

    /* flush out any stored-up bytes before we begin */
    int i = o_end - o_off;
    if (i > out_bytes)
      i = (int) out_bytes;
    if (i > 0) {
      output.write(o, o_off, i);
      o_off += i;
      offset += i;
      out_bytes -= i;
    }
    if (out_bytes == 0)
      return;

    /* restore local state */
    if (input_end) {
      if (bits_left != 16) {
        throw new IllegalStateException("previous pass overflowed " + bits_left + " bits");
      }
      if (bit_buffer != 0) {
        throw new IllegalStateException("non-empty overflowed buffer");
      }
      removeBits(bits_left);
      input_end = false;
    }

    long total = offset + out_bytes;
    end_frame = (int) (total / LZX_FRAME_SIZE) + (total % LZX_FRAME_SIZE > 0 ? 1 : 0);

    while (frame < end_frame) {

      /* have we reached the reset interval? (if there is one?) */
      if (reset_interval > 0 && ((frame % reset_interval) == 0)) {
        if (block_remaining > 0) {
          throw new IOException(String.format("%d bytes remaining at reset interval", block_remaining));
        }

        /* re-read the intel header and reset the huffman lengths */
        resetState();
      }

      /* LZX DELTA format has chunk_size, not present in LZX format */
      if (is_delta) {
        ensureBits(16);
        removeBits(16);
      }

      /* read header if necessary */
      if (!header_read) {
        /*
         * read 1 bit. if bit=0, intel filesize = 0. if bit=1, read intel filesize (32 bits)
         */
        j = 0;
        i = readBits(1);
        if (i > 0) {
          i = readBits(16);
          j = readBits(16);
        }
        intel_filesize = (i << 16) | j;
        header_read = true;
      }

      /*
       * calculate size of frame: all frames are 32k except the final frame which is 32kb or less. this can only be calculated
       * when lzx->length has been filled in.
       */
      frame_size = LZX_FRAME_SIZE;
      if (length > 0 && (length - offset) < frame_size) {
        frame_size = (int) (length - offset);
      }

      /* decode until one more frame is available */
      bytes_todo = frame_posn + frame_size - window_posn;

      while (bytes_todo > 0) {

        /* initialise new block, if one is needed */
        if (block_remaining == 0) {
          /* realign if previous block was an odd-sized UNCOMPRESSED block */
          if ((block_type == LZX_BLOCKTYPE_UNCOMPRESSED) && (block_length & 1) != 0) {
            readIfNeeded();
            i_off++;
          }

          /* read block type (3 bits) and block length (24 bits) */
          block_type = readBits(3);
          i = readBits(16);
          j = readBits(8);
          block_remaining = block_length = (i << 8) | j;
          /* D(("new block t%d len %u", lzx->block_type, lzx->block_length)) */

          /* read individual block headers */
          switch (block_type) {
            case LZX_BLOCKTYPE_ALIGNED:
              /* read lengths of and build aligned huffman decoding tree */
              for (i = 0; i < 8; i++) {
                alignedTree.len[i] = (short) readBits(3);
              }
              alignedTree.buildTable();
              /* no break -- rest of aligned header is same as verbatim */
            case LZX_BLOCKTYPE_VERBATIM:
              /* read lengths of and build main huffman decoding tree */
              mainTree.readLengths(0, LZX_NUM_CHARS);
              mainTree.readLengths(LZX_NUM_CHARS, LZX_NUM_CHARS + num_offsets);
              mainTree.buildTable();
              /* if the literal 0xE8 is anywhere in the block... */
              if (mainTree.len[0xE8] != 0)
                intel_started = true;
              /* read lengths of and build lengths huffman decoding tree */
              lengthTree.readLengths(0, LZX_NUM_SECONDARY_LENGTHS);
              lengthTree.buildTableMaybeEmpty();
              break;

            case LZX_BLOCKTYPE_UNCOMPRESSED:
              /* because we can't assume otherwise */
              intel_started = true;

              /* read 1-16 (not 0-15) bits to align to bytes */
              if (bits_left == 0)
                ensureBits(16);
              bits_left = 0;
              bit_buffer = 0;

              /* read 12 bytes of stored R0 / R1 / R2 values */
              for (i = 0; i < 12; i++) {
                readIfNeeded();
                buf[i] = inbuf[i_off++];
              }
              R0 = (buf[0] & 0xFF) | ((buf[1] & 0xFF) << 8) | ((buf[2] & 0xFF) << 16) | ((buf[3] & 0xFF) << 24);
              R1 = (buf[4] & 0xFF) | ((buf[5] & 0xFF) << 8) | ((buf[6] & 0xFF) << 16) | ((buf[7] & 0xFF) << 24);
              R2 = (buf[8] & 0xFF) | ((buf[9] & 0xFF) << 8) | ((buf[10] & 0xFF) << 16) | ((buf[11] & 0xFF) << 24);
              break;

            default:
              throw new IllegalStateException("bad block type");
          }
        }

        /*
         * decode more of the block: run = min(what's available, what's needed)
         */
        this_run = block_remaining;
        if (this_run > bytes_todo)
          this_run = bytes_todo;

        /* assume we decode exactly this_run bytes, for now */
        bytes_todo -= this_run;
        block_remaining -= this_run;

        /* decode at least this_run bytes */
        switch (block_type) {

          case LZX_BLOCKTYPE_VERBATIM:
            while (this_run > 0) {
              main_element = mainTree.readHuffSym();
              if (main_element < LZX_NUM_CHARS) {
                /* literal: 0 to LZX_NUM_CHARS-1 */
                window[window_posn++] = (byte) main_element;
                this_run--;
              } else {
                /* match: LZX_NUM_CHARS + ((slot<<3) | length_header (3 bits)) */
                main_element -= LZX_NUM_CHARS;

                /* get match length */
                match_length = main_element & LZX_NUM_PRIMARY_LENGTHS;
                if (match_length == LZX_NUM_PRIMARY_LENGTHS) {
                  if (lengthTree.empty) {
                    throw new IllegalStateException("LENGTH symbol needed but tree is empty");
                  }
                  length_footer = lengthTree.readHuffSym();
                  match_length += length_footer;
                }
                match_length += LZX_MIN_MATCH;

                /* get match offset */
                switch ((match_offset = (main_element >>> 3))) {
                  case 0:
                    match_offset = R0;
                    break;
                  case 1:
                    match_offset = R1;
                    R1 = R0;
                    R0 = match_offset;
                    break;
                  case 2:
                    match_offset = R2;
                    R2 = R0;
                    R0 = match_offset;
                    break;
                  case 3:
                    match_offset = 1;
                    R2 = R1;
                    R1 = R0;
                    R0 = match_offset;
                    break;
                  default:
                    extra = (match_offset >= 36) ? 17 : extra_bits[match_offset];
                    verbatim_bits = readBits(extra);
                    match_offset = position_base[match_offset] - 2 + verbatim_bits;
                    R2 = R1;
                    R1 = R0;
                    R0 = match_offset;
                }

                /* LZX DELTA uses max match length to signal even longer match */
                if (match_length == LZX_MAX_MATCH && is_delta) {
                  int extraLen = 0;
                  ensureBits(3); /* 4 entry huffman tree */
                  if (peekBits(1) == 0) {
                    removeBits(1); /* '0' -> 8 extra length bits */
                    extraLen = readBits(8);
                  } else if (peekBits(2) == 2) {
                    removeBits(2); /* '10' -> 10 extra length bits + 0x100 */
                    extraLen = readBits(10);
                    extraLen += 0x100;
                  } else if (peekBits(3) == 6) {
                    removeBits(3); /* '110' -> 12 extra length bits + 0x500 */
                    extraLen = readBits(12);
                    extraLen += 0x500;
                  } else {
                    removeBits(3); /* '111' -> 15 extra length bits */
                    extraLen = readBits(15);
                  }
                  match_length += extraLen;
                }

                if ((window_posn + match_length) > window_size) {
                  throw new IOException("match ran over window wrap");
                }

                /* copy match */
                rundest = window_posn;
                i = match_length;
                /* does match offset wrap the window? */
                if (match_offset > window_posn) {
                  if (match_offset > offset &&
                      (match_offset - window_posn) > ref_data_size) {
                    throw new IOException("match offset beyond LZX stream");
                  }
                  /* j = length from match offset to end of window */
                  j = match_offset - window_posn;
                  if (j > (int) window_size) {
                    throw new IOException("match offset beyond window boundaries");
                  }
                  runsrc = window_size - j;
                  if (j < i) {
                    /* if match goes over the window edge, do two copy runs */
                    i -= j;
                    while (j-- > 0)
                      window[rundest++] = window[runsrc++];
                    runsrc = 0;
                  }
                  while (i-- > 0)
                    window[rundest++] = window[runsrc++];
                } else {
                  runsrc = rundest - match_offset;
                  while (i-- > 0)
                    window[rundest++] = window[runsrc++];
                }

                this_run -= match_length;
                window_posn += match_length;
              }
            } /* while (this_run > 0) */
            break;

          case LZX_BLOCKTYPE_ALIGNED:
            while (this_run > 0) {
              main_element = mainTree.readHuffSym();
              if (main_element < LZX_NUM_CHARS) {
                /* literal: 0 to LZX_NUM_CHARS-1 */
                window[window_posn++] = (byte) main_element;
                this_run--;
              } else {
                /* match: LZX_NUM_CHARS + ((slot<<3) | length_header (3 bits)) */
                main_element -= LZX_NUM_CHARS;

                /* get match length */
                match_length = main_element & LZX_NUM_PRIMARY_LENGTHS;
                if (match_length == LZX_NUM_PRIMARY_LENGTHS) {
                  if (lengthTree.empty) {
                    throw new IllegalStateException("LENGTH symbol needed but tree is empty");
                  }
                  length_footer = lengthTree.readHuffSym();
                  match_length += length_footer;
                }
                match_length += LZX_MIN_MATCH;

                /* get match offset */
                switch ((match_offset = (main_element >>> 3))) {
                  case 0:
                    match_offset = R0;
                    break;
                  case 1:
                    match_offset = R1;
                    R1 = R0;
                    R0 = match_offset;
                    break;
                  case 2:
                    match_offset = R2;
                    R2 = R0;
                    R0 = match_offset;
                    break;
                  default:
                    extra = (match_offset >= 36) ? 17 : extra_bits[match_offset];
                    match_offset = position_base[match_offset] - 2;
                    if (extra > 3) {
                      /* verbatim and aligned bits */
                      extra -= 3;
                      verbatim_bits = readBits(extra);
                      match_offset += (verbatim_bits << 3);
                      aligned_bits = alignedTree.readHuffSym();
                      match_offset += aligned_bits;
                    } else if (extra == 3) {
                      /* aligned bits only */
                      aligned_bits = alignedTree.readHuffSym();
                      match_offset += aligned_bits;
                    } else if (extra > 0) { /* extra==1, extra==2 */
                      /* verbatim bits only */
                      verbatim_bits = readBits(extra);
                      match_offset += verbatim_bits;
                    } else /* extra == 0 */ {
                      /* ??? not defined in LZX specification! */
                      match_offset = 1;
                    }
                    /* update repeated offset LRU queue */
                    R2 = R1;
                    R1 = R0;
                    R0 = match_offset;
                }

                /* LZX DELTA uses max match length to signal even longer match */
                if (match_length == LZX_MAX_MATCH && is_delta) {
                  int extraLen = 0;
                  ensureBits(3); /* 4 entry huffman tree */
                  if (peekBits(1) == 0) {
                    removeBits(1); /* '0' -> 8 extra length bits */
                    extraLen = readBits(8);
                  } else if (peekBits(2) == 2) {
                    removeBits(2); /* '10' -> 10 extra length bits + 0x100 */
                    extraLen = readBits(10);
                    extraLen += 0x100;
                  } else if (peekBits(3) == 6) {
                    removeBits(3); /* '110' -> 12 extra length bits + 0x500 */
                    extraLen = readBits(12);
                    extraLen += 0x500;
                  } else {
                    removeBits(3); /* '111' -> 15 extra length bits */
                    extraLen = readBits(15);
                  }
                  match_length += extraLen;
                }

                if ((window_posn + match_length) > window_size) {
                  throw new IOException("match ran over window wrap");
                }

                /* copy match */
                rundest = window_posn;
                i = match_length;
                /* does match offset wrap the window? */
                if (match_offset > window_posn) {
                  if (match_offset > offset && (match_offset - window_posn) > ref_data_size) {
                    throw new IOException("match offset beyond LZX stream");
                  }
                  /* j = length from match offset to end of window */
                  j = match_offset - window_posn;
                  if (j > window_size) {
                    throw new IOException("match offset beyond window boundaries");
                  }
                  runsrc = window_size - j;
                  if (j < i) {
                    /* if match goes over the window edge, do two copy runs */
                    i -= j;
                    while (j-- > 0)
                      window[rundest++] = window[runsrc++];
                    runsrc = 0;
                  }
                  while (i-- > 0)
                    window[rundest++] = window[runsrc++];
                } else {
                  runsrc = rundest - match_offset;
                  while (i-- > 0)
                    window[rundest++] = window[runsrc++];
                }

                this_run -= match_length;
                window_posn += match_length;
              }
            } /* while (this_run > 0) */
            break;

          case LZX_BLOCKTYPE_UNCOMPRESSED:
            /*
             * as this_run is limited not to wrap a frame, this also means it won't wrap the window (as the window is a multiple of
             * 32k)
             */
            rundest = window_posn;
            window_posn += this_run;
            while (this_run > 0) {
              if ((i = i_end - i_off) == 0) {
                readIfNeeded();
              } else {
                if (i > this_run)
                  i = this_run;
                System.arraycopy(inbuf, i_off, window, rundest, i);
                rundest += i;
                i_off += i;
                this_run -= i;
              }
            }
            break;

          default:
            throw new IllegalStateException("bad block type"); /* might as well */
        }

        /* did the final match overrun our desired this_run length? */
        if (this_run < 0) {
          if (-this_run > block_remaining) {
            throw new IOException(String.format("overrun went past end of block by %d (%d remaining)", -this_run, block_remaining));
          }
          block_remaining -= -this_run;
        }

      } /* while (bytes_todo > 0) */

      /* streams don't extend over frame boundaries */
      if ((window_posn - frame_posn) != frame_size) {
        throw new IOException(String.format("decode beyond output frame limits! %d != %d", window_posn - frame_posn, frame_size));
      }

      /* re-align input bitstream */
      if (bits_left > 0)
        ensureBits(16);
      if ((bits_left & 15) != 0)
        removeBits(bits_left & 15);

      /* check that we've used all of the previous frame first */
      if (o_off != o_end) {
        throw new IOException(String.format("%d avail bytes, new %d frame", o_end - o_off, frame_size));
      }

      /* does this intel block _really_ need decoding? */
      if (intel_started && intel_filesize != 0 &&
          (frame <= 32768) && (frame_size > 10)) {
        byte[] data = e8_buf;
        int datastart = 0;
        int dataend = frame_size - 10;
        int curpos = intel_curpos;
        int filesize = intel_filesize;
        int absOff, relOff;

        /* copy e8 block to the e8 buffer and tweak if needed */
        o = data;
        o_off = 0;
        o_end = frame_size;
        System.arraycopy(window, frame_posn, data, 0, frame_size);

        while (datastart < dataend) {
          if ((data[datastart++] & 0xFF) != 0xE8) {
            curpos++;
            continue;
          }
          absOff = (data[datastart] & 0xFF) | ((data[datastart + 1] & 0xFF) << 8) | ((data[datastart + 2] & 0xFF) << 16) | ((data[datastart + 3] & 0xFF) << 24);
          if ((absOff >= -curpos) && (absOff < filesize)) {
            relOff = (absOff >= 0) ? absOff - curpos : absOff + filesize;
            data[datastart + 0] = (byte) (relOff & 0xff);
            data[datastart + 1] = (byte) ((relOff >>> 8) & 0xff);
            data[datastart + 2] = (byte) ((relOff >>> 16) & 0xff);
            data[datastart + 3] = (byte) ((relOff >>> 24) & 0xff);
          }
          datastart += 4;
          curpos += 5;
        }
        intel_curpos += frame_size;
      } else {
        o = window;
        o_off = frame_posn;
        o_end = frame_posn + frame_size;
        if (intel_filesize != 0)
          intel_curpos += frame_size;
      }

      /* write a frame */
      i = (out_bytes < frame_size) ? (int) out_bytes : frame_size;
      output.write(o, o_off, i);
      o_off += i;
      offset += i;
      out_bytes -= i;

      /* advance frame start position */
      frame_posn += frame_size;
      frame++;

      /* wrap window / frame position pointers */
      if (window_posn == window_size)
        window_posn = 0;
      if (frame_posn == window_size)
        frame_posn = 0;

    } /* while (lzx->frame < end_frame) */

    if (out_bytes > 0) {
      throw new IOException("bytes left to output");
    }
  }

  /*
   * make_decode_table(nsyms, nbits, length[], table[])
   *
   * This function was originally coded by David Tritscher. It builds a fast huffman decoding table from a canonical
   * huffman code lengths table.
   *
   * nsyms = total number of symbols in this huffman tree. nbits = any symbols with a code length of nbits or less can be
   * decoded in one lookup of the table. length = A table to get code lengths from [0 to nsyms-1] table = The table to
   * fill up with decoded symbols and pointers. Should be ((1<<nbits) + (nsyms*2)) in length.
   *
   * Returns 0 for OK or 1 for error
   */
  private static boolean makeDecodeTable(int nsyms, int nbits, short[] length, short[] table) {

    int sym, next_symbol;
    int leaf, fill;
    int bit_num;
    int pos = 0; /* the current position in the decode table */
    int table_mask = 1 << nbits;
    int bit_mask = table_mask >>> 1; /* don't do 0 length codes */

    /* fill entries for codes short enough for a direct mapping */
    for (bit_num = 1; bit_num <= nbits; bit_num++) {
      for (sym = 0; sym < nsyms; sym++) {
        if (length[sym] != bit_num)
          continue;
        leaf = pos;

        if ((pos += bit_mask) > table_mask)
          return false; /* table overrun */

        /* fill all possible lookups of this symbol with the symbol itself */
        for (fill = bit_mask; fill-- > 0;)
          table[leaf++] = (short) sym;
      }
      bit_mask >>>= 1;
    }

    /* exit with success if table is now complete */
    if (pos == table_mask)
      return true;

    /* mark all remaining table entries as unused */
    for (sym = pos; sym < table_mask; sym++) {
      table[sym] = (short) -1;
    }

    /* next_symbol = base of allocation for long codes */
    next_symbol = ((table_mask >>> 1) < nsyms) ? nsyms : (table_mask >>> 1);

    /*
     * give ourselves room for codes to grow by up to 16 more lzx. codes now start at bit nbits+16 and end at
     * (nbits+16-codelength)
     */
    pos <<= 16;
    table_mask <<= 16;
    bit_mask = 1 << 15;

    for (bit_num = nbits + 1; bit_num <= HUFF_MAXBITS; bit_num++) {
      for (sym = 0; sym < nsyms; sym++) {
        if (length[sym] != bit_num)
          continue;
        if (pos >= table_mask)
          return false; /* table overflow */

        leaf = pos >>> 16;
        for (fill = 0; fill < (bit_num - nbits); fill++) {
          /* if this path hasn't been taken yet, 'allocate' two entries */
          if (table[leaf] == -1) {
            table[(next_symbol << 1)] = (short) -1;
            table[(next_symbol << 1) + 1] = (short) -1;
            table[leaf] = (short) next_symbol++;
          }

          /* follow the path and select either left or right for next bit */
          leaf = table[leaf] << 1;
          if (((pos >>> (15 - fill)) & 1) != 0)
            leaf++;
        }
        table[leaf] = (short) sym;
        pos += bit_mask;
      }
      bit_mask >>>= 1;
    }

    /* full table? */
    return pos == table_mask;
  }

  private void readLens(short[] lens, int first, int last) throws IOException {
    int x, y, z;

    /* read lengths for pretree (20 symbols, lengths stored in fixed 4 bits) */
    for (x = 0; x < 20; x++) {
      y = readBits(4);
      preTree.len[x] = (short) y;
    }
    preTree.buildTable();

    for (x = first; x < last;) {
      z = preTree.readHuffSym();
      if (z == 17) {
        /* code = 17, run of ([read 4 bits]+4) zeros */
        y = readBits(4);
        y += 4;
        while (y-- > 0)
          lens[x++] = 0;
      } else if (z == 18) {
        /* code = 18, run of ([read 5 bits]+20) zeros */
        y = readBits(5);
        y += 20;
        while (y-- > 0)
          lens[x++] = 0;
      } else if (z == 19) {
        /* code = 19, run of ([read 1 bit]+4) [read huffman symbol] */
        y = readBits(1);
        y += 4;
        z = preTree.readHuffSym();
        z = lens[x] - z;
        if (z < 0)
          z += 17;
        while (y-- > 0)
          lens[x++] = (short) z;
      } else {
        /* code = 0 to 16, delta current length entry */
        z = lens[x] - z;
        if (z < 0)
          z += 17;
        lens[x++] = (short) z;
      }
    }
  }

  private void ensureBits(int nbits) throws IOException {
    while (bits_left < (nbits)) {
      readBytes();
    }
  }

  private void readBytes() throws IOException {
    readIfNeeded();
    int b0 = inbuf[i_off++] & 0xff;
    readIfNeeded();
    int b1 = inbuf[i_off++] & 0xff;
    int val = (b1 << 8) | b0;
    injectBits(val, 16);
  }

  private void readIfNeeded() throws IOException {
    if (i_off >= i_end) {
      readInput();
    }
  }

  private void readInput() throws IOException {
    int l = inbuf.length;
    int read = input.read(inbuf, 0, l);

    /*
     * we might overrun the input stream by asking for bits we don't use, so fake 2 more bytes at the end of input
     */
    if (read <= 0) {
      if (input_end) {
        throw new IOException("out of input bytes");
      } else {
        read = 2;
        inbuf[0] = inbuf[1] = 0;
        input_end = true;
      }
    }

    /* update i_ptr and i_end */
    i_off = 0;
    i_end = read;
  }

  private int readBits(int nbits) throws IOException {
    ensureBits(nbits);
    int val = peekBits(nbits);
    removeBits(nbits);
    return val;
  }

  private int peekBits(int nbits) {
    return bit_buffer >>> (BITBUF_WIDTH - nbits);
  }

  private void removeBits(int nbits) {
    bit_buffer <<= nbits;
    bits_left -= nbits;
  }

  private void injectBits(int bitdata, int nbits) {
    bit_buffer |= bitdata << (BITBUF_WIDTH - nbits - bits_left);
    bits_left += nbits;
  }

  private class HuffTable {
    final String tbl;
    final int tableBits;
    final int maxSymbols;
    final short[] table;
    final short[] len;
    boolean empty;

    HuffTable(String tbl, int maxSymbols, int tableBits) {
      this.tbl = tbl;
      this.maxSymbols = maxSymbols;
      this.tableBits = tableBits;
      table = new short[(1 << tableBits) + (maxSymbols * 2)];
      len = new short[maxSymbols + LZX_LENTABLE_SAFETY];
    }

    void buildTable() {
      if (!makeDecodeTable(maxSymbols, tableBits, len, table)) {
        throw new IllegalStateException(String.format("failed to build %s table", tbl));
      }
      empty = false;
    }

    void buildTableMaybeEmpty() {
      empty = false;
      if (!makeDecodeTable(maxSymbols, tableBits, len, table)) {
        for (int i = 0; i < maxSymbols; i++) {
          if (len[i] > 0) {
            throw new IllegalStateException(String.format("failed to build %s table", tbl));
          }
        }
        /* empty tree - allow it, but don't decode symbols with it */
        empty = true;
      }
    }

    void readLengths(int first, int last) throws IOException {
      readLens(len, first, last);
    }

    int readHuffSym() throws IOException {
      ensureBits(HUFF_MAXBITS);
      int sym = table[peekBits(tableBits)] & 0xFFFF;
      if (sym >= maxSymbols)
        sym = huffTraverse(sym);
      removeBits(len[sym]);
      return sym;
    }

    int huffTraverse(int sym) {
      int i = 1 << (BITBUF_WIDTH - tableBits);
      do {
        if ((i >>>= 1) == 0) {
          throw new IllegalStateException("huffTraverse");
        }
        sym = table[(sym << 1) | (((bit_buffer & i) != 0) ? 1 : 0)];
      } while (sym >= maxSymbols);
      return sym;
    }
  }
}
