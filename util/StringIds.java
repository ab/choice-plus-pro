

package util;

/**
 * This class exists solely as a place to put string ids.
 * "Abstract" is to prevent initialization.
 */
public abstract class StringIds
{
   public static final int NULL_TOKEN = -1;

   // Candidate statuses
   public static final int UNDEFINED = 109;
   public static final int ELECTED = 110;
   public static final int DEFEATED = 111;
   public static final int CONTINUING = 112;
   public static final int EXCLUDED = 113;
   public static final int UNDEFEATABLE = 114;
   public static final int SPECIAL = 115;

   // Basic concepts
   public static final int COUNT = 201;
   public static final int THRESHOLD = 202;
   public static final int ELIMINATED = 203; 

   // CPPro commands
   public static final int START_CPPRO_COMMANDS    = 401; //Used in DataLoader
   public static final int COM_BALFORM_FIELDS      = 401;
   public static final int COM_BALFORM_SEPS        = 402;
   public static final int COM_CAMB_VAC_RECOUNT    = 403;
   public static final int COM_CANDIDATE           = 404;
   public static final int COM_WRITE_IN            = 405;
   public static final int COM_COMPLY_WITH         = 406;
   public static final int COM_DBL_ENTRY_VER       = 407;
   public static final int COM_ELECT               = 408;  
   public static final int COM_ELIMINATE           = 409;
   public static final int COM_EXCLUDE_CAND        = 410;
   public static final int COM_FINAL_PILE          = 411;
   public static final int COM_INCLUDE             = 412;
   public static final int COM_INITDROP            = 413;
   public static final int COM_NON_CAND            = 414;
   public static final int COM_OFFICE              = 415;
   public static final int COM_SORT_BALLOTS        = 416;
   public static final int COM_SPECIAL             = 417;
   public static final int COM_START_VERIF         = 418;
   public static final int COM_SURPLUS             = 419;
   public static final int COM_SYSTEM              = 420;
   //public static final int COM_TEST              = 420;
   //public static final int COM_TEST_RESULT       = 421;
   public static final int COM_THRESHOLD           = 422;
   public static final int COM_TIES                = 423;
   public static final int COM_TITLE               = 424;
   public static final int COM_USE                 = 425;
   public static final int COM_VALID               = 426;
   public static final int COM_AUTOTEST            = 427;
   public static final int COM_AUTOTEST_RESULTS    = 428;
   public static final int COM_IGNORE_COMMANDS     = 429;
   public static final int COM_UNDEFEATABLE        = 430;
   public static final int COM_IGNORE_COMMAND      = 431; // Special; only set in DataLoader.getCommand()
   public static final int COM_DISTRIBUTED_COUNT   = 432;
   public static final int COM_CONTEST             = 433;
   public static final int COM_STATISTICS          = 434;
   public static final int COM_TRANSFER            = 435;
   public static final int COM_SIMULTANEOUS_DROP   = 436;
   public static final int END_PRMASTER_COMMANDS   = 437; //Used in DataLoader

   public static final int START_CPPRO_MODIFIERS   = 502; //Used in DataLoader
// public static final int MOD_ALLBALLOTS          = 501;
   public static final int MOD_ASCENDING           = 502; //.SORT {field-name} ASCENDING
   public static final int MOD_AVOID_EXH           = 503;
   public static final int MOD_BAL_ID_ALPHA        = 504;
   public static final int MOD_BAL_ID_N            = 505; // ballot id numeric
   public static final int MOD_BAL_TOP_A           = 506;
   public static final int MOD_BAL_VALUE           = 507;
// public static final int MOD_BUCKLIN             = 508;
   public static final int MOD_CAMBRIDGE           = 509;
   public static final int MOD_CANDIDS_NR          = 510;
// public static final int MOD_CANDIDS_NR          = 511;
   public static final int MOD_RANKINGS_ALPHA      = 512; // ballot format field choice: rankings, alphanumeric
   public static final int MOD_CANDNUM             = 513;
   public static final int MOD_DROOP               = 514;
   public static final int MOD_DUP                 = 515;
   public static final int MOD_EVERY_NTH_CAMBRIDGE = 516;
   public static final int MOD_FRACT               = 517;
   public static final int MOD_HARE                = 518;
   public static final int MOD_HUMAN               = 519;
   public static final int MOD_IGNORE_FIELD        = 520;
// public static final int MOD_LASTBATCH           = 521;
   public static final int MOD_LESSTHAN            = 522;
   public static final int MOD_NEW_YORK_CITY       = 523;
   public static final int MOD_NODUP               = 524;
   public static final int MOD_OFF                 = 525;
   public static final int MOD_ON                  = 526;
   public static final int MOD_PREC_NUM_A          = 527;
   public static final int MOD_PREVRND             = 528;
   public static final int MOD_RAND                = 529;
   public static final int MOD_RANDOM_COMP         = 530;
   public static final int MOD_SIMULXFERWINNERS    = 531;
   public static final int MOD_SIMULXFERWINNERSNOT = 532;
   public static final int MOD_SORT_FIELD          = 533;
   public static final int MOD_STOP_THRESH         = 534;
   public static final int MOD_STV                 = 535;
   public static final int MOD_IRV                 = 536;
   public static final int MOD_IR                  = 537;
   public static final int MOD_BALLOT_MEASURE      = 538;
   public static final int MOD_IRELAND             = 539;
   public static final int MOD_BURLINGTON_IRV      = 540; // modify .COMPLY-WITH
   public static final int MOD_EXHAUST_CONTINUING_DUPES = 541; // modify .TRANSFER
   public static final int MOD_EXHAUST_ANY_DUPES   = 542; // modify .TRANSFER
   public static final int MOD_CONTINUE_TILL_2     = 543; // modify .TRANSFER
   public static final int MOD_ONE_PREVRND         = 544; // modify .TIES
   public static final int END_CPPRO_MODIFIERS     = 545; //Used in DataLoader


   // Error handling
   // (IDs 1000-2999 reserved)
   public static final int FATAL_ERROR = 1000;
   public static final int WARNING = 2000;

   // Fatal errors
   public static final int FILE_NOT_FOUND = 1001;
   public static final int DATA_FORMAT_EXCEPTION = 1002; // "DFE"
   public static final int DFE_VALUE_FOR_VOTES_EXPECTED = 1003;
}
