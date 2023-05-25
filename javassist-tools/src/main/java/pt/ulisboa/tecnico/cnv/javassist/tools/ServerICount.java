package pt.ulisboa.tecnico.cnv.javassist.tools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static Map<Long, Long> saved_ninsts = new HashMap<Long, Long>();

    public ServerICount(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    public static void incBasicBlock(int position, int length, long id) {
        nblocks++;
        ninsts += length;
        long new_i;
        if (saved_ninsts.get(id) == null) {
            new_i = length;
        } else {
            new_i = saved_ninsts.get(id) + length;
        }
        //System.out.println("NEW VALUE OF INSTRUCTIONS " + new_i + "OF THREAD - " + id);
        saved_ninsts.put(id, new_i);
    }

    public static void incBehavior(String name) {
        nmethods++;
    }

    public static void resetVariables(long id) {
        nblocks = ninsts = nmethods = 0;
        System.out.println("RESETING INSTRUCTIONS OF THREAD - " + id);
        saved_ninsts.put(id, 0L);
    }

    public static void inProcess() {
        System.out.println("JUST FINISHED PROCESSING IMAGE");
    }

    public static long getInstructions(long id) {
        return saved_ninsts.get(id);
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
            //behavior.insertBefore(String.format("%s.resetVariables(Thread.currentThread().getId());", ServerICount.class.getName()));
        }

        if (behavior.getName().equals("runSimulation")) {
            behavior.insertBefore(String.format("%s.resetVariables(Thread.currentThread().getId());", ServerICount.class.getName()));
            behavior.insertAfter(String.format("%s.printStatistics();", ServerICount.class.getName()));
            //behavior.insertAfter(String.format("%s.saveInstructions();", ServerICount.class.getName()));
            behavior.insertAfter(String.format("bruhInstructions(%s.getInstructions(Thread.currentThread().getId()));",  ServerICount.class.getName()));
        }

        if (behavior.getName().equals("war")) {
            behavior.insertBefore(String.format("%s.resetVariables(Thread.currentThread().getId());", ServerICount.class.getName()));
            behavior.insertAfter(String.format("%s.printStatistics();", ServerICount.class.getName()));
            //behavior.insertAfter(String.format("%s.saveInstructions();", ServerICount.class.getName()));
            behavior.insertAfter(String.format("bruhInstructions(%s.getInstructions(Thread.currentThread().getId()));",  ServerICount.class.getName()));
        }

        if (behavior.getName().equals("process")) {
            behavior.insertBefore(String.format("%s.resetVariables(Thread.currentThread().getId());", ServerICount.class.getName()));
            behavior.insertAfter(String.format("%s.printStatistics();", ServerICount.class.getName()));
            //behavior.insertAfter(String.format("%s.saveInstructions();", ServerICount.class.getName()));
            behavior.insertAfter(String.format("bruhInstructions(%s.getInstructions(Thread.currentThread().getId()));",  ServerICount.class.getName()));
        }

        

        /* if (behavior.getName().equals("process")) {
            behavior.insertBefore(String.format("%s.resetVariables(Thread.currentThread().getId());", ServerICount.class.getName()));
            behavior.insertAfter(String.format("%s.inProcess();", ServerICount.class.getName()));
            behavior.insertAfter(String.format("%s.printStatistics();", ServerICount.class.getName()));
        } */
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        block.behavior.insertAfter(String.format("long idxxx = Thread.currentThread().getId();%s.incBasicBlock(%s, %s, idxxx);", ServerICount.class.getName(), block.getPosition(), block.getLength()));
    }

}
