package pt.ulisboa.tecnico.cnv.javassist.tools;

import java.util.List;

import javassist.CannotCompileException;
import javassist.CtBehavior;

public class ServerICount extends CodeDumper {

    /**
     * Number of executed basic blocks.
     */
    private static long nblocks = 0;

    /**
     * Number of executed methods.
     */
    private static long nmethods = 0;

    /**
     * Number of executed instructions.
     */
    private static long ninsts = 0;

    public ServerICount(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    public static void incBasicBlock(int position, int length) {
        nblocks++;
        ninsts += length;
    }

    public static void incBehavior(String name) {
        nmethods++;
    }

    public static void resetVariables() {
        nblocks = ninsts = nmethods = 0;
    }

    public static void inProcess() {
        System.out.println("JUST FINISHED PROCESSING IMAGE");
    }

    public static void printStatistics() {
        System.out.println(String.format("[%s] Number of executed methods: %s", ServerICount.class.getSimpleName(), nmethods));
        System.out.println(String.format("[%s] Number of executed basic blocks: %s", ServerICount.class.getSimpleName(), nblocks));
        System.out.println(String.format("[%s] Number of executed instructions: %s", ServerICount.class.getSimpleName(), ninsts));
    }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        super.transform(behavior);
        behavior.insertAfter(String.format("%s.incBehavior(\"%s\");", ServerICount.class.getName(), behavior.getLongName()));

        if (behavior.getName().equals("main")) {
            behavior.insertAfter(String.format("%s.printStatistics();", ServerICount.class.getName()));
            resetVariables();
        }

        if (behavior.getName().equals("handle")) {
            behavior.insertBefore(String.format("%s.resetVariables();", ServerICount.class.getName()));
            behavior.insertAfter(String.format("%s.printStatistics();", ServerICount.class.getName()));
        }

        /* if (behavior.getName().equals("process")) {
            behavior.insertBefore(String.format("%s.resetVariables();", ServerICount.class.getName()));
            behavior.insertAfter(String.format("%s.inProcess();", ServerICount.class.getName()));
            behavior.insertAfter(String.format("%s.printStatistics();", ServerICount.class.getName()));
        } */
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        block.behavior.insertAt(block.line, String.format("%s.incBasicBlock(%s, %s);", ServerICount.class.getName(), block.getPosition(), block.getLength()));
    }

}
