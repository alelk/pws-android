package com.alelk.pws.xmlengine;

/**
 * Created by alelkin on 25.03.2015.
 */
public abstract class Constants {

    /**
     * Supported PWS XML tags
     */
    public final static class TAG {
        public final static class PSLM {
            public static final String TAG = "psalm";
            public static final String AUTHOR = "author";
            public static final String TRANSLATOR = "translator";
            public static final String VERSION = "version";
            public static final String NAME = "name";
            public static final String NUMBERS = "numbers";
            public static final String COMPOSER = "composer";
            public static final String YEAR = "year";
            public static final String TEXT = "text";
            public final static class NUMS {
                public static final String TAG = Constants.TAG.PSLM.NUMBERS;
                public static final String NUMBER = "number";
            }
            public final static class TXT {
                public static final String TAG = PSLM.TEXT;
                public static final String VERSE = "verse";
                public static final String CHORUS = "chorus";
                public final static class VRS {
                    public static final String NUMBER = "number";
                }
                public final static class CRS {
                    public static final String NUMBER = "number";
                }
            }
        }
        public final static class BK {
            public static final String TAG = "book";
            public static final String EDITION = "edition";
        }
    }

    /**
     * Supported tag attribute values
     */
    public final static class ATTR_VAL {
        public final static class BK {
            public static final String EDITION_GUSLI = "gusli";
            public static final String EDITION_PV2000 = "pv2000";
        }
    }
}
