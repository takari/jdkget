package io.takari.jdkget.win;

public class CabConstants {
  /* generic CAB definitions */

  /** Offset from start of cabinet to the reserved header data (if present). */
  public static final int MSCAB_HDR_RESV_OFFSET = 0x28;

  /** Cabinet header flag: cabinet has a predecessor */
  public static final int MSCAB_HDR_PREVCAB = 0x01;
  /** Cabinet header flag: cabinet has a successor */
  public static final int MSCAB_HDR_NEXTCAB = 0x02;
  /** Cabinet header flag: cabinet has reserved header space */
  public static final int MSCAB_HDR_RESV = 0x04;

  /** mscabd_file::attribs attribute: file is read-only. */
  public static final int MSCAB_ATTRIB_RDONLY = 0x01;
  /** mscabd_file::attribs attribute: file is hidden. */
  public static final int MSCAB_ATTRIB_HIDDEN = 0x02;
  /** mscabd_file::attribs attribute: file is an operating system file. */
  public static final int MSCAB_ATTRIB_SYSTEM = 0x04;
  /** mscabd_file::attribs attribute: file is "archived". */
  public static final int MSCAB_ATTRIB_ARCH = 0x20;
  /** mscabd_file::attribs attribute: file is an executable program. */
  public static final int MSCAB_ATTRIB_EXEC = 0x40;
  /** mscabd_file::attribs attribute: filename is UTF8, not ISO-8859-1. */
  public static final int MSCAB_ATTRIB_UTF_NAME = 0x80;

  /** mscab_decompressor::set_param() parameter: search buffer size. */
  public static final int MSCABD_PARAM_SEARCHBUF = 0;
  /** mscab_decompressor::set_param() parameter: repair MS-ZIP streams? */
  public static final int MSCABD_PARAM_FIXMSZIP = 1;
  /** mscab_decompressor::set_param() parameter: size of decompression buffer */
  public static final int MSCABD_PARAM_DECOMPBUF = 2;


  /** Compression mode: no compression. */
  public static final int MSCAB_COMP_NONE = 0;
  /** Compression mode: MSZIP (deflate) compression. */
  public static final int MSCAB_COMP_MSZIP = 1;
  /** Compression mode: Quantum compression */
  public static final int MSCAB_COMP_QUANTUM = 2;
  /** Compression mode: LZX compression */
  public static final int MSCAB_COMP_LZX = 3;

  /* structure offsets */
  public static final int cfhead_Signature = 0x00;
  public static final int cfhead_CabinetSize = 0x08;
  public static final int cfhead_FileOffset = 0x10;
  public static final int cfhead_MinorVersion = 0x18;
  public static final int cfhead_MajorVersion = 0x19;
  public static final int cfhead_NumFolders = 0x1A;
  public static final int cfhead_NumFiles = 0x1C;
  public static final int cfhead_Flags = 0x1E;
  public static final int cfhead_SetID = 0x20;
  public static final int cfhead_CabinetIndex = 0x22;
  public static final int cfhead_SIZEOF = 0x24;
  public static final int cfheadext_HeaderReserved = 0x00;
  public static final int cfheadext_FolderReserved = 0x02;
  public static final int cfheadext_DataReserved = 0x03;
  public static final int cfheadext_SIZEOF = 0x04;
  public static final int cffold_DataOffset = 0x00;
  public static final int cffold_NumBlocks = 0x04;
  public static final int cffold_CompType = 0x06;
  public static final int cffold_SIZEOF = 0x08;
  public static final int cffile_UncompressedSize = 0x00;
  public static final int cffile_FolderOffset = 0x04;
  public static final int cffile_FolderIndex = 0x08;
  public static final int cffile_Date = 0x0A;
  public static final int cffile_Time = 0x0C;
  public static final int cffile_Attribs = 0x0E;
  public static final int cffile_SIZEOF = 0x10;
  public static final int cfdata_CheckSum = 0x00;
  public static final int cfdata_CompressedSize = 0x04;
  public static final int cfdata_UncompressedSize = 0x06;
  public static final int cfdata_SIZEOF = 0x08;

  /* flags */
  public static final int cffoldCOMPTYPE_MASK = 0x000f;
  public static final int cffoldCOMPTYPE_NONE = 0x0000;
  public static final int cffoldCOMPTYPE_MSZIP = 0x0001;
  public static final int cffoldCOMPTYPE_QUANTUM = 0x0002;
  public static final int cffoldCOMPTYPE_LZX = 0x0003;
  public static final int cfheadPREV_CABINET = 0x0001;
  public static final int cfheadNEXT_CABINET = 0x0002;
  public static final int cfheadRESERVE_PRESENT = 0x0004;
  public static final int cffileCONTINUED_FROM_PREV = 0xFFFD;
  public static final int cffileCONTINUED_TO_NEXT = 0xFFFE;
  public static final int cffileCONTINUED_PREV_AND_NEXT = 0xFFFF;

  /*
   * CAB data blocks are <= 32768 bytes in uncompressed form. Uncompressed blocks have zero growth. MSZIP guarantees that
   * it won't grow above uncompressed size by more than 12 bytes. LZX guarantees it won't grow more than 6144 bytes.
   * Quantum has no documentation, but the largest block seen in the wild is 337 bytes above uncompressed size.
   */
  public static final int CAB_BLOCKMAX = 32768;
  public static final int CAB_INPUTMAX = CAB_BLOCKMAX + 6144;

  /*
   * There are no more than 65535 data blocks per folder, so a folder cannot be more than 32768*65535 bytes in length. As
   * files cannot span more than one folder, this is also their max offset, length and offset+length limit.
   */
  public static final int CAB_FOLDERMAX = 65535;
  public static final long CAB_LENGTHMAX = (long) CAB_BLOCKMAX * CAB_FOLDERMAX;
}
