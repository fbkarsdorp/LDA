import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import topicmodels.DDLLDA;
import topicmodels.LDA;
import topicmodels.LLDA;
import topicmodels.ProtoLDA;
import util.Corpus;
import util.ProtoTopics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;


public class Main {

    public static void main(String[] args) throws ArgumentParserException, IOException, ClassNotFoundException {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("(DD)(L)LDA")
                .defaultHelp(true)
                .description("Simple implementation of LDA, LLDA and DDLLDA.");

        parser.addArgument("-f", "--file")
                .dest("file")
                .type(String.class)
                .help("The filename from which to read training or testing instances.");

        parser.addArgument("-o", "--output")
                .dest("output")
                .type(String.class)
                .help("The output directory.");

        parser.addArgument("-s", "--system")
                .dest("system")
                .type(String.class)
                .choices("LDA", "LLDA", "DDLLDA")
                .help("The model to use for training or inference (LDA, LLDA, DDLLDA)");

        parser.addArgument("-m", "--model")
                .dest("model")
                .type(String.class)
                .help("The filename pointing to the model learned during training.");

        parser.addArgument("-i", "--iterations")
                .dest("iterations")
                .type(Integer.class)
                .help("The number of iterations for Gibbs sampling.");

        parser.addArgument("--nTopics")
                .dest("numTopics")
                .type(Integer.class)
                .setDefault(100)
                .help("The number of topics to use in LDA.");

        parser.addArgument("--protoTopics")
                .dest("prototopics")
                .type(String.class)
                .help("The filename pointing to the file holding the proto-topics");

        parser.addArgument("--alpha")
                .dest("alpha")
                .type(Double.class)
                .setDefault(0.01)
                .help("Alpha parameter: smooting over topic distribution.");

        parser.addArgument("--beta")
                .dest("beta")
                .type(Double.class)
                .setDefault(0.01)
                .help("Beta parameter: smoothing over unigram distribution.");

        parser.addArgument("--gamma")
                .dest("gamma")
                .type(Double.class)
                .setDefault(0.01)
                .help("Gamma parameter: smoothing over the topic distribution.");

        Namespace ns = parser.parseArgs(args);
        Integer numTopics = ns.getInt("numTopics");
        Double alpha = ns.getDouble("alpha");
        Double beta = ns.getDouble("beta");
        Double gamma = ns.getDouble("gamma");
        Integer iterations = ns.getInt("iterations");
        String file = ns.getString("file");
        String model = ns.getString("model");
        String system = ns.getString("system");
        String output = ns.getString("output");
        String protoTopicFile = ns.getString("prototopics");

        File outputDirectory;
        if (output == null && model != null) {
            outputDirectory = new File(new File(model).getParent());
        } else if (ns.getString("output") != null) {
            outputDirectory = new File(ns.getString("output"));
        } else {
            throw new IOException("No output directory given");
        }
        if (!outputDirectory.exists()) { outputDirectory.mkdir(); }

        if (model == null) {
            Corpus corpus = new Corpus();
            corpus.readFile(file);
            if (system.equals("LLDA")) {
                LLDA llda = new LLDA(alpha, beta, corpus);
                llda.train(iterations, corpus);
                llda.writeTopicDistributions(new File(outputDirectory + File.separator + "final-topics.txt"), corpus, 0.0);
                llda.write(new File(outputDirectory + File.separator + "model.lda"));
            } else if (system.equals("DDLLDA")) {
                DDLLDA ddllda = new DDLLDA(alpha, beta, gamma, corpus);
                ddllda.train(iterations, corpus);
                ddllda.writeTopicDistributions(new File(outputDirectory + File.separator + "final-topics.txt"), corpus, 0.0);
                ddllda.write(new File(outputDirectory + File.separator + "model.lda"));
                ddllda.printTopicDistribution(new File(outputDirectory + File.separator + "topic-distribution.txt"));
            } else if (system.equals("ProtoLDA")) {
                HashMap<String, ArrayList<String>> protoTopics = ProtoTopics.read(protoTopicFile);
                ProtoLDA lda = new ProtoLDA(numTopics, alpha, beta, gamma, corpus, protoTopics);
                lda.train(iterations, corpus);
                lda.writeTopicDistributions(new File(outputDirectory + File.separator + "final-topics.txt"), corpus, 0.0);
            } else {
                LDA lda = new LDA (numTopics, alpha, beta, corpus);
                lda.train(iterations, corpus);
                lda.writeTopicDistributions(new File(outputDirectory + File.separator + "final-topics.txt"), corpus, 0.0);
                lda.write(new File(outputDirectory + File.separator + "model.lda"));
            }

        } else {
            if (system.equals("LLDA")) {
                LLDA llda = LLDA.read(new File(model));
                Corpus corpus = new Corpus(llda.wordIndex, llda.topicIndex);
                corpus.readFile(file);
                llda.infer(iterations, corpus);
                llda.writeTopicDistributions(new File(outputDirectory + File.separator + "inference-topics.txt"), corpus, alpha);

            } else if (system.equals("DDLLDA")) {
                DDLLDA ddllda = DDLLDA.read(new File(model));
                Corpus corpus = new Corpus(ddllda.wordIndex, ddllda.topicIndex, ddllda.typeIndex);
                corpus.readFile(file);
                ddllda.infer(iterations, corpus);
                ddllda.writeTopicDistributions(new File(outputDirectory + File.separator + "inference-topics.txt"), corpus, gamma);
            } else {
                LDA lda = LDA.read(new File(model));
                Corpus corpus = new Corpus(lda.wordIndex, lda.topicIndex);
                corpus.readFile(file);
                lda.infer(iterations, corpus);
                lda.writeTopicDistributions(new File(outputDirectory + File.separator + "inference-topics.txt"), corpus, alpha);
            }
        }
    }
}
