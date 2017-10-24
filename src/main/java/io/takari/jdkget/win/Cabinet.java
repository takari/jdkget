package io.takari.jdkget.win;

import static io.takari.jdkget.win.CabConstants.*;
import static io.takari.jdkget.win.CabIO.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Partial java port of https://github.com/kyz/libmspack commit 6037317dac8ebf04289a60f04f983db12f8236b0
 */
public class Cabinet {

  CabInput in;

  /**
   * The next cabinet in a chained list, if this cabinet was opened with mscab_decompressor::search(). May be NULL to mark
   * the end of the list.
   */
  Cabinet next;

  /**
   * The filename of the cabinet. More correctly, the filename of the physical file that the cabinet resides in. This is
   * given by the library user and may be in any format.
   */
  String filename;

  /** The file offset of cabinet within the physical file it resides in. */
  long base_offset;

  /** The length of the cabinet file in bytes. */
  long length;

  /** The previous cabinet in a cabinet set, or NULL. */
  Cabinet prevcab;

  /** The next cabinet in a cabinet set, or NULL. */
  Cabinet nextcab;

  /** The filename of the previous cabinet in a cabinet set, or NULL. */
  String prevname;

  /** The filename of the next cabinet in a cabinet set, or NULL. */
  String nextname;

  /**
   * The name of the disk containing the previous cabinet in a cabinet set, or NULL.
   */
  String previnfo;

  /**
   * The name of the disk containing the next cabinet in a cabinet set, or NULL.
   */
  String nextinfo;

  /** A list of all files in the cabinet or cabinet set. */
  CabFile[] files;

  /** A list of all folders in the cabinet or cabinet set. */
  CabFolder[] folders;

  /**
   * The set ID of the cabinet. All cabinets in the same set should have the same set ID.
   */
  int set_id;

  /**
   * The index number of the cabinet within the set. Numbering should start from 0 for the first cabinet in the set, and
   * increment by 1 for each following cabinet.
   */
  int set_index;

  /**
   * The number of bytes reserved in the header area of the cabinet.
   *
   * If this is non-zero and flags has MSCAB_HDR_RESV set, this data can be read by the calling application. It is of the
   * given length, located at offset (base_offset + MSCAB_HDR_RESV_OFFSET) in the cabinet file.
   *
   * @see flags
   */
  int header_resv;

  /**
   * Header flags.
   *
   * - MSCAB_HDR_PREVCAB indicates the cabinet is part of a cabinet set, and has a predecessor cabinet. -
   * MSCAB_HDR_NEXTCAB indicates the cabinet is part of a cabinet set, and has a successor cabinet. - MSCAB_HDR_RESV
   * indicates the cabinet has reserved header space.
   *
   * @see prevname, previnfo, nextname, nextinfo, header_resv
   */
  int flags;

  long blocks_off; /* offset to data blocks */

  int block_resv; /* reserved space in data blocks */

  public Cabinet(CabInput in) throws IOException {
    this.in = in;
    cabd_read_headers();
  }

  public Cabinet getNext() {
    return next;
  }

  static class CabFolder {
    /**
     * A pointer to the next folder in this cabinet or cabinet set, or NULL if this is the final folder.
     */
    CabFolder next;

    /**
     * The compression format used by this folder.
     *
     * The macro MSCABD_COMP_METHOD() should be used on this field to get the algorithm used. The macro MSCABD_COMP_LEVEL()
     * should be used to get the "compression level".
     *
     * @see MSCABD_COMP_METHOD(), MSCABD_COMP_LEVEL()
     */
    int comp_type;

    /**
     * The total number of data blocks used by this folder. This includes data blocks present in other files, if this folder
     * spans more than one cabinet.
     */
    int num_blocks;

    CabFile merge_prev; /* first file needing backwards merge */

    CabFile merge_next; /* first file needing forwards merge */

    CabFolderData data = new CabFolderData();

    /**
     * Returns the compression method used by a folder.
     *
     * @param comp_type a mscabd_folder::comp_type value
     * @return one of #MSCAB_COMP_NONE, #MSCAB_COMP_MSZIP, #MSCAB_COMP_QUANTUM or #MSCAB_COMP_LZX
     */
    int getCompMethod() {
      return comp_type & 0x0F;
    }

    /**
     * Returns the compression level used by a folder.
     *
     * @param comp_type a mscabd_folder::comp_type value
     * @return the compression level. This is only defined by LZX and Quantum compression
     */
    int getCompLevel() {
      return (comp_type >> 8) & 0x1F;
    }
  }

  /* there is one of these for every cabinet a folder spans */
  static class CabFolderData {
    CabFolderData next;

    Cabinet cab;

    long offset; /* cabinet offset of first datablock */
  }

  static class CabFile {
    /**
     * The next file in the cabinet or cabinet set, or NULL if this is the final file.
     */
    CabFile next;

    /**
     * The filename of the file.
     *
     * A null terminated string of up to 255 bytes in length, it may be in either ISO-8859-1 or UTF8 format, depending on
     * the file attributes.
     *
     * @see attribs
     */
    String filename;

    /** The uncompressed length of the file, in bytes. */
    long length;

    /**
     * File attributes.
     *
     * The following attributes are defined: - #MSCAB_ATTRIB_RDONLY indicates the file is write protected. -
     * #MSCAB_ATTRIB_HIDDEN indicates the file is hidden. - #MSCAB_ATTRIB_SYSTEM indicates the file is a operating system
     * file. - #MSCAB_ATTRIB_ARCH indicates the file is "archived". - #MSCAB_ATTRIB_EXEC indicates the file is an executable
     * program. - #MSCAB_ATTRIB_UTF_NAME indicates the filename is in UTF8 format rather than ISO-8859-1.
     */
    int attribs;

    /** File's last modified time, hour field. */
    int time_h;
    /** File's last modified time, minute field. */
    int time_m;
    /** File's last modified time, second field. */
    int time_s;

    /** File's last modified date, day field. */
    int date_d;
    /** File's last modified date, month field. */
    int date_m;
    /** File's last modified date, year field. */
    int date_y;

    /** A pointer to the folder that contains this file. */
    CabFolder folder;

    /** The uncompressed offset of this file in its folder. */
    long offset;
  }

  public List<CabEntry> entries() {
    List<CabEntry> entries = new ArrayList<>();

    for (CabFile cf : files) {
      entries.add(new CabEntry(this, cf));
    }

    return entries;
  }

  /***************************************
   * CABD_READ_HEADERS
   ***************************************
   * reads the cabinet file header, folder list and file list. fills out a pre-existing mscabd_cabinet structure,
   * allocates memory for folders and files as necessary
   */
  void cabd_read_headers() throws IOException {
    int num_folders, num_files, folder_resv, i, x;
    byte[] buf = new byte[64];

    /* initialise pointers */
    this.base_offset = in.getOffset();

    /* read in the CFHEADER */
    if (in.read(buf, 0, cfhead_SIZEOF) != cfhead_SIZEOF) {
      throw new IOException("Error reading header");
    }

    /* check for "MSCF" signature */
    if (EndGetI32(buf, cfhead_Signature) != 0x4643534D) {
      throw new IOException("Invalid signature");
    }

    /* some basic header fields */
    this.length = EndGetL32(buf, cfhead_CabinetSize);
    this.set_id = EndGetI16(buf, cfhead_SetID);
    this.set_index = EndGetI16(buf, cfhead_CabinetIndex);

    /* get the number of folders */
    num_folders = EndGetI16(buf, cfhead_NumFolders);
    if (num_folders == 0) {
      throw new IOException("no folders in cabinet.");
    }

    /* get the number of files */
    num_files = EndGetI16(buf, cfhead_NumFiles);
    if (num_files == 0) {
      throw new IOException("no files in cabinet.");
    }

    /* check cabinet version */
    if ((buf[cfhead_MajorVersion] != 1) && (buf[cfhead_MinorVersion] != 3)) {
      // warn("WARNING; cabinet version is not 1.3");
    }

    /* read the reserved-sizes part of header, if present */
    this.flags = EndGetI16(buf, cfhead_Flags);
    if ((this.flags & cfheadRESERVE_PRESENT) != 0) {
      if (in.read(buf, 0, cfheadext_SIZEOF) != cfheadext_SIZEOF) {
        throw new IOException("Error reading headerExt");
      }
      this.header_resv = EndGetI16(buf, cfheadext_HeaderReserved);
      folder_resv = buf[cfheadext_FolderReserved];
      this.block_resv = buf[cfheadext_DataReserved];

      if (this.header_resv > 60000) {
        // warn("WARNING; reserved header > 60000.");
      }

      /* skip the reserved header */
      if (this.header_resv != 0) {
        in.seekRelative(this.header_resv);
      }
    } else {
      this.header_resv = 0;
      folder_resv = 0;
      this.block_resv = 0;
    }

    /* read name and info of preceeding cabinet in set, if present */
    if ((this.flags & cfheadPREV_CABINET) != 0) {
      this.prevname = cabd_read_string();
      this.previnfo = cabd_read_string();
    }

    /* read name and info of next cabinet in set, if present */
    if ((this.flags & cfheadNEXT_CABINET) != 0) {
      this.nextname = cabd_read_string();
      this.nextinfo = cabd_read_string();
    }


    /* read folders */
    folders = new CabFolder[num_folders];
    for (i = 0; i < num_folders; i++) {
      if (in.read(buf, 0, cffold_SIZEOF) != cffold_SIZEOF) {
        throw new IOException("Error reading folder");
      }
      if (folder_resv != 0) {
        in.seekRelative(folder_resv);
      }

      CabFolder f = new CabFolder();
      f.comp_type = EndGetI16(buf, cffold_CompType);
      f.num_blocks = EndGetI16(buf, cffold_NumBlocks);
      f.data.offset = this.base_offset + EndGetL32(buf, cffold_DataOffset);
      f.data.cab = this;

      /* link folder into list of folders */
      if (i > 0) {
        folders[i - 1].next = f;
      }
      folders[i] = f;
    }

    /* read files */
    files = new CabFile[num_files];
    for (i = 0; i < num_files; i++) {
      if (in.read(buf, 0, cffile_SIZEOF) != cffile_SIZEOF) {
        throw new IOException("Error reading file");
      }

      CabFile file = new CabFile();
      file.length = EndGetL32(buf, cffile_UncompressedSize);
      file.attribs = EndGetI16(buf, cffile_Attribs);
      file.offset = EndGetL32(buf, cffile_FolderOffset);

      /* set folder pointer */
      x = EndGetI16(buf, cffile_FolderIndex);
      if (x < cffileCONTINUED_FROM_PREV) {
        file.folder = folders[i];

        if (i < 0 || i >= folders.length) {
          throw new IOException("invalid folder index");
        }
      } else {
        /*
         * either CONTINUED_TO_NEXT, CONTINUED_FROM_PREV or CONTINUED_PREV_AND_NEXT
         */
        if ((x == cffileCONTINUED_TO_NEXT) || (x == cffileCONTINUED_PREV_AND_NEXT)) {
          /* get last folder */
          CabFolder fol = folders[folders.length - 1];
          file.folder = fol;

          /* set "merge next" pointer */
          if (fol.merge_next == null)
            fol.merge_next = file;
        }

        if ((x == cffileCONTINUED_FROM_PREV) || (x == cffileCONTINUED_PREV_AND_NEXT)) {
          /* get first folder */
          CabFolder fol = folders[0];
          file.folder = fol;

          /* set "merge prev" pointer */
          if (fol.merge_prev == null)
            fol.merge_prev = file;
        }
      }

      /* get time */
      x = EndGetI16(buf, cffile_Time);
      file.time_h = x >> 11;
      file.time_m = (x >> 5) & 0x3F;
      file.time_s = (x << 1) & 0x3E;

      /* get date */
      x = EndGetI16(buf, cffile_Date);
      file.date_d = x & 0x1F;
      file.date_m = (x >> 5) & 0xF;
      file.date_y = (x >> 9) + 1980;

      /* get filename */
      file.filename = cabd_read_string();

      /* link file entry into file list */
      if (i > 0) {
        files[i - 1].next = file;
      }
      files[i] = file;
    }
  }

  String cabd_read_string() throws IOException {

    byte[] buf = new byte[256];
    int len = 0;

    /* read up to 256 bytes */
    /* search for a null terminator */
    do {
      buf[len] = (byte) in.read();
    } while (buf[len] != 0 && len++ < 256);

    /* reject empty strings */
    if (len == 0) {
      throw new IOException("Empty string read");
    }
    /* no null terminator in 256 bytes */
    if (buf[len] != 0) {
      throw new IOException("Error reading string");
    }

    return new String(buf, 0, len, "UTF-8");
  }

  /***************************************
   * CABD_SEARCH, CABD_FIND
   ***************************************
   * cabd_search opens a file, finds its extent, allocates a search buffer, then reads through the whole file looking for
   * possible cabinet headers. if it finds any, it tries to read them as real cabinets. returns a linked list of results
   *
   * cabd_find is the inner loop of cabd_search, to make it easier to break out of the loop and be sure that all resources
   * are freed
   */
  public static List<Cabinet> cabd_search(CabInput in) throws IOException {
    return cabd_search(in, 32768);
  }

  public static List<Cabinet> cabd_search(CabInput in, int searchBufLength) throws IOException {
    if (!in.negativeSeekSupported()) {
      throw new IllegalArgumentException("Search requires negative seek support");
    }
    if (in.getLength() == -1L) {
      throw new IllegalArgumentException("Search requires length availability");
    }

    /* allocate a search buffer */
    byte[] search_buf = new byte[searchBufLength];

    /* open file and get its full file length */
    List<Cabinet> cabs = new ArrayList<>();
    cabd_find(search_buf, in, cabs);
    return cabs;
  }

  static void cabd_find(byte[] buf, CabInput in, List<Cabinet> cabs) throws IOException {
    Cabinet cab;
    long caboff, offset, flen = in.getLength();
    int length;
    byte[] p;
    int poff, pend, state = 0;
    long cablen_u32 = 0, foffset_u32 = 0;
    int false_cabs = 0;

    /* search through the full file length */
    for (offset = 0; offset < flen; offset += length) {
      /* fill the search buffer with data from disk */
      length = in.read(buf, 0, buf.length);

      /* FAQ avoidance strategy */
      if ((offset == 0) && (EndGetI32(buf, 0) == 0x28635349)) {
        // warn("WARNING; found InstallShield header. This is probably an InstallShield file. Use UNSHIELD from www.synce.org to
        // unpack it.");
      }

      /* read through the entire buffer. */
      for (p = buf, poff = 0, pend = length; poff < pend;) {
        switch (state) {
          /* starting state */
          case 0:
            /*
             * we spend most of our time in this while loop, looking for a leading 'M' of the 'MSCF' signature
             */
            while (poff < pend && p[poff] != 0x4D)
              poff++;
            /* if we found tht 'M', advance state */
            if (poff++ < pend)
              state = 1;
            break;

          /* verify that the next 3 bytes are 'S', 'C' and 'F' */
          case 1:
            state = ((p[poff++] & 0xFF) == 0x53) ? 2 : 0;
            break;
          case 2:
            state = ((p[poff++] & 0xFF) == 0x43) ? 3 : 0;
            break;
          case 3:
            state = ((p[poff++] & 0xFF) == 0x46) ? 4 : 0;
            break;

          /* we don't care about bytes 4-7 (see default: for action) */

          /* bytes 8-11 are the overall length of the cabinet */
          case 8:
            cablen_u32 = (p[poff++] & 0xFF);
            state++;
            break;
          case 9:
            cablen_u32 |= (p[poff++] & 0xFF) << 8;
            state++;
            break;
          case 10:
            cablen_u32 |= (p[poff++] & 0xFF) << 16;
            state++;
            break;
          case 11:
            cablen_u32 |= (p[poff++] & 0xFF) << 24;
            state++;
            break;

          /* we don't care about bytes 12-15 (see default: for action) */

          /* bytes 16-19 are the offset within the cabinet of the filedata */
          case 16:
            foffset_u32 = (p[poff++] & 0xFF);
            state++;
            break;
          case 17:
            foffset_u32 |= (p[poff++] & 0xFF) << 8;
            state++;
            break;
          case 18:
            foffset_u32 |= (p[poff++] & 0xFF) << 16;
            state++;
            break;
          case 19:
            foffset_u32 |= (p[poff++] & 0xFF) << 24;
            /*
             * now we have recieved 20 bytes of potential cab header. work out the offset in the file of this potential cabinet
             */
            caboff = offset + poff - 20;

            /* should reading cabinet fail, restart search just after 'MSCF' */
            offset = caboff + 4;

            /*
             * check that the files offset is less than the alleged length of the cabinet, and that the offset + the alleged length
             * are 'roughly' within the end of overall file length
             */
            if ((foffset_u32 < cablen_u32) && ((caboff + foffset_u32) < (flen + 32)) && ((caboff + cablen_u32) < (flen + 32))) {
              /* likely cabinet found -- try reading it */
              long savedOff = in.getOffset();
              in.seekAbsolute(caboff);
              try {
                cab = new Cabinet(in);
              } catch (Exception e) {
                false_cabs++;
                cab = null;
              }
              in.seekAbsolute(savedOff);
              if (cab != null) {
                /* cabinet read correctly! */

                /* link the cab into the list */
                if (!cabs.isEmpty()) {
                  cabs.get(cabs.size() - 1).next = cab;
                }
                cabs.add(cab);

                /* cause the search to restart after this cab's data. */
                offset = caboff + cablen_u32;
              }
            }

            /* restart search */
            if (offset >= flen) {
              return;
            }

            in.seekAbsolute(offset);
            length = 0;
            poff = pend;
            state = 0;
            break;

          /* for bytes 4-7 and 12-15, just advance state/pointer */
          default:
            poff++;
            state++;
        } /* switch(state) */
      } /* for (... p < pend ...) */
    } /* for (... offset < length ...) */

    if (false_cabs > 0) {
      //warn("%d false cabinets found", false_cabs);
    }
  }
}
