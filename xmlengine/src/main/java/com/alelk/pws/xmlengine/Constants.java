package com.alelk.pws.xmlengine;

/**
 * Created by alelkin on 25.03.2015.
 * Constants of XmlEngine module.
 */
public interface Constants {

    /**
     * Supported PWS XML tags
     */
    public final static class TAG {
        public final static class PSLM {
            public static final String EDITION = BK.EDITION;
            public static final String TAG = "psalm";
            public static final String AUTHOR = "author";
            public static final String TRANSLATOR = "translator";
            public static final String VERSION = "version";
            public static final String NAME = "name";
            public static final String NUMBERS = "numbers";
            public static final String COMPOSER = "composer";
            public static final String YEAR = "year";
            public static final String TEXT = "text";
            public static final String TONALITIES = "tonalities";
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
            public final static class TONS {
                public static final String TAG = PSLM.TONALITIES;
                public static final String TONALITY = "tonality";
            }
        }
        public final static class BK {
            public static final String TAG = "book";
            public static final String VERSION = "version";
            public static final String NAME = "name";
            public static final String SHORT_NAME = "shortname";
            public static final String DISPLAY_NAME = "displayname";
            public static final String EDITION = "edition";
            public static final String DESCRIPTION = "description";
            public static final String RELEASE_DATE = "releasedate";
            public static final String COMMENT = "comment";
            public static final String AUTHORS = "authors";
            public static final String CREATORS = "creators";
            public static final String REVIEWERS = "reviewers";
            public static final String EDITORS = "editors";
            public static final String PSALMS = "psalms";
            public static final String CHAPTERS = "chapters";
            public final static class CRTRS {
                public static final String TAG = BK.CREATORS;
                public static final String CREATOR = "creator";
            }
            public final static class RVRS {
                public static final String TAG = BK.REVIEWERS;
                public static final String REVIEWER = "reviewer";
            }
            public final static class EDTRS {
                public static final String TAG = BK.EDITORS;
                public static final String EDITOR = "editor";
            }
            public final static class AUTRS {
                public static final String TAG = BK.AUTHORS;
                public static final String AUTHOR = "author";
            }
            public final static class PSLMS {
                public static final String TAG = BK.PSALMS;
                public static final String PSALM = PSLM.TAG;
                public static final String REF = "ref";
            }
            public final static class CHPRS {
                public static final String TAG = BK.CHAPTERS;
                public static final String CHAPTER = "chapter";
                public final static class CHPTR {
                    public static final String TAG = CHPRS.CHAPTER;
                    public static final String NUMBER = "number";
                    public static final String NAME = "name";
                    public static final String SHORT_NAME = "shortname";
                    public static final String VERSION = "version";
                    public static final String DISPLAY_NAME = "displayname";
                    public static final String DESCRIPTION = "description";
                    public static final String RELEASE_DATE = "releasedate";
                    public static final String PSALM_NUMBERS = "psalmnumbers";
                }
            }
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
