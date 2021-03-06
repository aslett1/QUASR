package ReadsetProcessor;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import uk.ac.sanger.quasr.PEReadsetProcessor;
import uk.ac.sanger.quasr.SEReadsetProcessor;
import uk.ac.sanger.quasr.invariables.Invariables;
import uk.ac.sanger.quasr.version;

/**
 *
 * @author sw10
 * Takes in either a single-end or a paired-end read file, or 2 PE files.
 */
public class ReadsetProcessor {

    public static void main(String[] args) {
        String infile = null;
        String outprefix = null;
        String matefile = null;
        boolean isPaired = false;
        boolean gzipOutput = false;
        String mids = null;
        boolean demultiBySeq = false;
        String customMIDfile = null;
        boolean demultiByHead = false;
        String pattern = "#(\\d+)/\\d$";
        boolean removeDups = false;
        String primerfile = null;
        boolean doQC = false;
        int length = 50;
        float median = 20.0F;
        int midOffset = 10;
        int primerLeeway = 40;
        boolean doQA = false;
        String rPath = null;
        int numParse = 100;
        int windowLen = 50;
        String usage = "[USAGE]: readsetProcessor.jar [options]\n"
                + "General:\n"
                + "\t-h/--help\tPrint this usage information\n"
                + "\t-v/--version\tPrint version number\n"
                + "*\t-i/--infile\tSE or single-PE FASTQ (or GZIPed FASTQ) or SFF\n"
                + "\t-r/--reverse\tFASTQ (or GZIPed FASTQ) or SFF containing reverse mates\n"
                + "\t-2/--paired\tInput file is paired-end. Only necessary if 1 file parsed\n"
                + "*\t-o/--outprefix\tOutput directory and file prefix\n"
                + "\t-z/--gzip\tCompress output files to GZIPed FASTQ\n"
                + "\t-n/--num\tNumber of records to parse at a time [default: 100]\n"
                + "\t-I/--illumina\tQuality scores encoded with Illumina offset (+64) instead of Sanger (+33)\n"
                + "Demultiplex:\n"
                + "\t-M/--mids\tComma-separated MIDs to be parsed. Accepts ranges\n"
                + "+\t-s/--sequence\tDemultiplex by parsing sequence. Mutually-exlusive with -d\n"
                + "\t-c/--custom\tFile containing custom MID sequences. Only allowed with -s\n"
                + "\t-O/--offset\tMaximum offset MID can be within a read. Only allowed with -s [default: 10]\n"
                + "+\t-H/--header\tDemultiplex by parsing header. Mutually exclusive with -s\n"
                + "\t-P/--pattern\tRegex to match in header. Only allowed with -d [default: \"#(\\d+)/\\d$\"]\n"
                + "Duplicate removal:\n"
                + "\t-d/--duplicate\tPerform duplicate removal\n"
                + "Primer removal:\n"
                + "\t-p/--primer\tFile containing primer sequences\n"
                + "\t-L/--leeway\tMaximum distance primer can be within a read [default: 40]\n"
                + "Quality control:\n"
                + "\t-q/--quality\tPerform quality control\n"
                + "\t-l/--length\tMinimum read length cutoff [default: 50]\n"
                + "\t-m/--median\tMedian read quality cutoff [default: 20.0]\n"
                + "QA graphing:\n"
                + "\t-g/--graph\tPerform quality assurance graphing\n"
                + "\t-R/--Rpath\tPath to R binary (only needs to be set once if current stored path doesn't work)\n"
                + "\t-w/--window\tWindow length for 3'-cross sectional dropoff [default:50]\n"
                + "\n"
                + "[NOTE]: Options marked * are mandatory. Those marked + are mandatory but mutually-exclusive\n"
                + "All others are optional. All steps are optional. Steps are performed in the order shown.\n";

        Options options = new Options();
        options.addOption("h", "help", false, "Print this usage information");
        options.addOption("v", "version", false, "Print version number");
        options.addOption("i", "infile", true, "SE or single-PE FASTQ (or GZIPed FASTQ) or SFF file");
        options.addOption("r", "reverse", true, "FASTQ (or GZIPed FASTQ) or SFF containing reverse mates");
        options.addOption("2", "paired", false, "Input file is paired-end. Only necessary if 1 file parsed");
        options.addOption("M", "mids", true, "Comma-separated MIDs to be parsed. Accepts ranges");
        options.addOption("o", "outprefix", true, "Output directory and file prefix");
        options.addOption("O", "offset", true, "Maximum offset MID can be within a read. Only allowed with -s [default: 10]");
        options.addOption("L", "leeway", true, "Maximum distance primer can be within a read [default: 10]");
        options.addOption("s", "sequence", false, "Demultiplex by sequence. Mutually exlusive with -d/--header");
        options.addOption("H", "header", false, "Demultiplex by header. Mutually exlusive with -s/--sequence");
        options.addOption("P", "pattern", true, "Regex to match in header if using -d [Default=\"#(\\d+)/\\d$\"]");
        options.addOption("c", "custom", true, "File containing custom MID sequences. Only allowed with -s/--sequence");
        options.addOption("p", "primer", true, "File containing primer sequences");
        options.addOption("z", "gzip", false, "Compress output files to GZIPed FASTQ");
        options.addOption("I", "illumina", false, "Quality scores encoded with Illumina offset (+64) instead of Sanger (+33)");
        options.addOption("l", "length", true, "Minimum read length cutoff [default: 50]");
        options.addOption("m", "median", true, "Minimum median-read-quality cutoff [default: 20.0]");
        options.addOption("n", "num", true, "Number of records to parse at a time [default: 100]");
        options.addOption("d", "duplicate", false, "Perform duplicate removal");
        options.addOption("q", "quality", false, "Perform quality control");
        options.addOption("g", "graph", false, "Perform quality assurance graphing");
        options.addOption("R", "Rpath", true, "Path to R binary (only needs to be set once if current stored path doesn't work)");
        options.addOption("w", "window", true, "Window length for 3'-cross sectional dropoff [default:50]");

        if (args.length == 0) {
            System.out.println(usage);
            System.exit(0);
        }

        CommandLineParser parser = new BasicParser();
        try {
            CommandLine commandLine = parser.parse(options, args);
            if (commandLine.hasOption('h')) {
                System.out.println(usage);
                System.exit(0);
            }
            if (commandLine.hasOption('v')) {
                System.out.println(version.version);
                System.exit(0);
            }
            if (commandLine.hasOption('i')) {
                infile = commandLine.getOptionValue('i');
            }
            if (commandLine.hasOption('r')) {
                matefile = commandLine.getOptionValue('r');
                isPaired = true;
            }
            if (commandLine.hasOption('2')) {
                isPaired = true;
            }
            if (commandLine.hasOption('o')) {
                outprefix = commandLine.getOptionValue('o');
            }
            if (commandLine.hasOption('M')) {
                mids = commandLine.getOptionValue('M');
            }
            if (commandLine.hasOption('P')) {
                pattern = commandLine.getOptionValue('P');
            }
            if (commandLine.hasOption('s')) {
                demultiBySeq = true;
            }
            if (commandLine.hasOption('H')) {
                demultiByHead = true;
            }
            if (commandLine.hasOption('c')) {
                customMIDfile = commandLine.getOptionValue('c');
            }
            if (commandLine.hasOption('p')) {
                primerfile = commandLine.getOptionValue('p');
            }
            if (commandLine.hasOption('z')) {
                gzipOutput = true;
            }
            if (commandLine.hasOption('d')) {
                removeDups = true;
            }
            if (commandLine.hasOption('q')) {
                doQC = true;
            }
            if (commandLine.hasOption('g')) {
                doQA = true;
            }
            if (commandLine.hasOption('R')) {
                rPath = commandLine.getOptionValue('R');
            }
            if (commandLine.hasOption('w')) {
                windowLen = Integer.parseInt(commandLine.getOptionValue('w'));
            }
            if (commandLine.hasOption('l')) {
                length = Integer.parseInt(commandLine.getOptionValue('l'));
            }
            if (commandLine.hasOption('n')) {
                numParse = Integer.parseInt(commandLine.getOptionValue('n'));
            }
            if (commandLine.hasOption('m')) {
                median = Float.parseFloat(commandLine.getOptionValue('m'));
            }
            if (commandLine.hasOption('I')) {
                Invariables.SetASCIIOffset(64);
            }
            if (commandLine.hasOption('O')) {
                midOffset = Integer.parseInt(commandLine.getOptionValue('O'));
            }
            if (commandLine.hasOption('L')) {
                primerLeeway = Integer.parseInt(commandLine.getOptionValue('L'));
            }
        } catch (ParseException err) {
            System.err.println("[ERROR]: Unrecognised option: " + err.getMessage());
            System.exit(1);
        }

        if (infile == null) {
            System.err.println("[ERROR]: Must specify an input file");
            System.exit(1);
        } else if (outprefix == null) {
            System.err.println("[ERROR]: Must specify an output prefix");
            System.exit(1);
        }
        if (mids != null) {
            if (demultiBySeq == demultiByHead) {
                System.err.println("[ERROR]: Choose either -s or -d for demultiplexing");
                System.exit(1);
            }
        } else {
            if (demultiBySeq != false || demultiByHead != false) {
                System.err.println("[ERROR]: Must specify MIDs to parse");
                System.exit(1);
            }
        }

        try {
            if (isPaired == true) {
                PEReadsetProcessor pe = null;
                if (matefile == null) {
                    pe = new PEReadsetProcessor(infile, outprefix, gzipOutput);
                } else {
                    pe = new PEReadsetProcessor(infile, matefile, outprefix, gzipOutput);
                }
                if (removeDups == true) {
                    pe.runDuplicateRemoval();
                }
                if (primerfile != null) {
                    pe.addPrimerRemovalToPipeline(primerfile, primerLeeway);
                }
                if (doQC == true) {
                    pe.addQualityControlToPipeline(median, length);
                }
                if (doQA == true) {
                    pe.addQAGraphingToPipeline(windowLen, rPath);
                }
                pe.runPipeline();
                
            } else {
                SEReadsetProcessor se = new SEReadsetProcessor(infile, outprefix, gzipOutput);
                if (demultiByHead == true) {
                    se.runDemultiplexByHeader(pattern, mids);
                } else if (demultiBySeq == true) {
                    if (customMIDfile == null) {
                        se.runDemultiplexBySequence(mids, midOffset);
                    } else {
                        se.runDemultiplexBySequence(customMIDfile, mids, midOffset);
                    }
                }
                if (removeDups == true) {
                    se.runDuplicateRemoval();
                }
                if (primerfile != null) {
                    se.addPrimerRemovalToPipeline(primerfile, primerLeeway);
                }
                if (doQC == true) {
                    se.addQualityControlToPipeline(median, length);
                }
                if (doQA == true) {
                    se.addQAGraphingToPipeline(windowLen, rPath);
                }
                se.runPipeline();
            }
        } catch (IOException ex) {
            Logger.getLogger(ReadsetProcessor.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(ReadsetProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}