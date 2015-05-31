package edu.nyu.jet.ice.relation;

import edu.nyu.jet.ice.utils.IceUtils;

import java.io.IOException;

/**
 * Created by yhe on 2/26/15.
 */
public class BatchMaeToApf {

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Jet.RelationAL.BatchMaeToApf maeFileList txtFileList apfFileList");
            System.exit(-1);
        }
        String[] maeFiles = IceUtils.readLines(args[0]);
        String[] txtFiles = IceUtils.readLines(args[1]);
        String[] apfFiles = IceUtils.readLines(args[2]);
        if (maeFiles.length != apfFiles.length ||
                maeFiles.length != txtFiles.length) {
            System.err.println("Mae, txt, and apf file list should have the same length.");
        }
        for (int i = 0; i < maeFiles.length; i++) {
            System.err.println("Mae file:" + maeFiles[i]);
            MaeToApf.main(new String[]{maeFiles[i], txtFiles[i], apfFiles[i]});
        }
    }
}
