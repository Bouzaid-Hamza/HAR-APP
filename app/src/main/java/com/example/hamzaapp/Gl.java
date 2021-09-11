package com.example.hamzaapp;

import java.text.DecimalFormat;

public class Gl {
    public static final int ACC_BUFFER_CAPACITY = 4096;
    public static final int ACC_BlOCK_CAPACITY = 64;
    public static final int DATASET_CAPACITY = 10000;

    public static final String FFT_COEF_LABEL = "fft_coef_";
    public static final String DATA_SET_NAME = "accelerometer_features";
    public static final String MAX_LABEL = "max";
    public static final String CLASS_HEADER = "label";
    public static final String FEATURES_FILE_NAME = "features.arff";
    public static final String MODEL_FILE_NAME = "j48.model";
    public static final String SUMMARY_FILE_NAME = "summary.txt";
    public static final String HISTORY_FILE_NAME = "history.json";

    public static final String CLASS_STANDING = "standing";
    public static final String CLASS_WALKING = "walking";
    public static final String CLASS_RUNNING = "running";
    public static final String CLASS_OTHERS = "others";
    public static final String KEY_CLASS_LABEL = "checked_class";

    public static final String NOTIFICATION_TITLE = "Collector service";
    public static final String NOTIFICATION_CONTENT = "Collecting data ...";
    public static final String KEY_RESULT_RECEIVER = "result";

    public static final String FILE_CREATED_MSG = "Features file created";
    public static final String FILE_UPDATED_MSG = "Features file updated";
    public static final String FILE_ERROR_MSG = "Error: Couldn't create features file";
    public static final String FILE_DELETED_MSG = "features file deleted";
    public static final String CLASSIFY_ERROR_MSG = "Error: Couldn't predict your activity";
    public static final String OLD_FILE_ERROR_MSG = "Error: Couldn't update features file";
    public static final String MODEL_SAVED_MSG = "MODEL HAS BEEN SAVED";
    public static final String HISTORY_FILE_DELETED_MSG = "History file deleted";
    public static final String HISTORY_FILE_DELETE_ERROR = "Error: Couldn't reset history";
    public static final String MODEL_SAVED_ERROR = "Error: Couldn't save model";
    public static final String KEY_XYZ_VALUES = "xyz";

    public static final String PCT_CORR_INSTS = "Correctly Classified Instances : ";
    public static final String PCT_INCORR_INSTS = "Incorrectly Classified Instances : ";
    public static final String TOTAL_INSTS = "Total Number of Instances : ";

    public static final String KEY_COLLECTOR_STATE = "collector_activity_state";

    public static final DecimalFormat DF2 = new DecimalFormat("#.##");
    public static final DecimalFormat DF3 = new DecimalFormat("#.###");
    public static final DecimalFormat FFT_DF = new DecimalFormat("00");
}
