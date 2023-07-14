import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

public class srcPartC {
    //Needed variables for analysis. callGraph respresents the entire call graph data is read into.
    //Bug reports are the final reports to be printed, errors store needed information to create the bug reports.
    //Individual supports store each functions support value for support value for confidence calculation.
    //newCallGraph and finalCallGraph for recursion purposes to implement inter procedural
    static HashMap<String, ArrayList<String>> callGraph = new HashMap<>();
    static HashMap<String, Integer> functionUse = new HashMap<>();
    static ArrayList<BugReport> bugReports = new ArrayList<>();
    static ArrayList<Error> errors = new ArrayList<>();
    static HashMap<String, Integer> individualSupports = new HashMap<>();
    static HashMap<String, ArrayList<String>> newCallGraph = new HashMap<>();
    static HashMap<String, ArrayList<String>> finalCallGraph = new HashMap<>();

    public static void main(String[] args) {
        //functionPairsWithSupport stores the supports of pairs to calculate confidence.
        HashMap<String, Integer> functionPairsWithSupport = new HashMap<>();
        Integer supportHolder = 3;
        Integer confidenceHolder = 65;
        Integer depthHolder = 0;
        if(args.length>1) {
            supportHolder = Integer.parseInt(args[1]);
            confidenceHolder = Integer.parseInt(args[2]);
        }
        if(args.length>3) {
            depthHolder = Integer.parseInt(args[3]);
        }
        final Integer supportRequirement = supportHolder;
        final Integer confidenceRequirement = confidenceHolder;
        final Integer depth = depthHolder;
        try {
            File myObj = new File("call_graph.txt");
            Scanner myReader = new Scanner(myObj);
            String firstNode = "";
            //Loop to read call graph
            while(myReader.hasNextLine()) {
                firstNode = myReader.nextLine();
                if(firstNode.contains("Call graph node for function")) {
                    break;
                }
            }

            while (myReader.hasNextLine()) {
                if(firstNode.contains("Call graph node for function")) {
                    int firstOccurance = firstNode.indexOf("'");
                    int lastOccurance = firstNode.lastIndexOf("'");
                    String leftFunction = firstNode.substring(firstOccurance+1,lastOccurance); //Function the code is in
                    functionUse.put(leftFunction, Integer.parseInt(firstNode.substring(firstNode.indexOf("=")+1)));
                    firstNode = myReader.nextLine();
                    ArrayList<String> str = new ArrayList<String>();
                    callGraph.put(leftFunction,str);
                    while(firstNode.contains("calls function")||firstNode.contains("calls external node")) {
                        if(firstNode.contains("calls external node")) {
                            firstNode = myReader.nextLine();
                            continue;
                        }
                        firstOccurance = firstNode.indexOf("'");
                        lastOccurance = firstNode.lastIndexOf("'");
                        String rightFunction = firstNode.substring(firstOccurance+1,lastOccurance); //function being called
                        callGraph.get(leftFunction).add(rightFunction); //updates call graph
                        if (myReader.hasNextLine()) {
                            firstNode = myReader.nextLine();
                        } else
                            break;
                    }
                }
                if (!myReader.hasNextLine()) {
                    break;
                }
                firstNode = myReader.nextLine();
            }
            myReader.close();

            //newCallGraph = callGraph;

            callGraph.forEach(((t, u) -> newCallGraph.put(t, u) )); //updates newCallGraph value to callGraph value
            callGraph.forEach(((t, u) -> {
                rec(t, 0,depth); //Calls recurson
                finalCallGraph.put(t, newCallGraph.get(t)); //updates final call graph values with needed functions
            }));

            ArrayList<String> allFunctionsList = new ArrayList<>(functionUse.entrySet().stream().filter(x -> x.getValue() >= supportRequirement)
                    .map(Map.Entry::getKey).collect(Collectors.toList()));

            allFunctionsList.sort(Comparator.naturalOrder());

            //Check if we are getting unique pairs
            for (int i = 0; i < allFunctionsList.size() - 1; i++) {
                for (int j = i + 1; j < allFunctionsList.size(); j++) {
                    // call the support function to find the support and fill up the error_in field
                    int support = support(new String[]{allFunctionsList.get(i), allFunctionsList.get(j)});
                    functionPairsWithSupport.put(allFunctionsList.get(i)+allFunctionsList.get(j), support);
                }
            }

            for(int i = 0;i<errors.size();i++) {
                String buggedFunc = errors.get(i).buggyFunction;
                if( buggedFunc != "") {
                    String[] pair = (errors.get(i).functionPair);
                    String pairString = pair[0]+pair[1];
                    String functionWErr = errors.get(i).functionWithError;
                    if(pair!=null && functionPairsWithSupport.get(pairString)!=null && functionWErr!= null){

                        int supportPair = functionPairsWithSupport.get(pairString);
                        int individualSupport;
                        if(individualSupports.get(functionWErr)!=null) {
                            individualSupport = individualSupports.get(functionWErr);
                        }else {
                            individualSupport = getIndividualSupport(functionWErr);
                            individualSupports.put(functionWErr, individualSupport);
                        }
                        Double confidence = confidence(supportPair, individualSupport); //Get confidence with supports calculated above
                        if(confidence>=confidenceRequirement && supportPair>=supportRequirement) {
                            bugReports.add(new BugReport(pair, supportPair, functionWErr, buggedFunc, confidence)); //create all bug reports
                        }
                    }
                }
            }
            bugReports.forEach((BugReport bugRep)-> {
                System.out.println("bug: "+bugRep.functionWithError+" in "+bugRep.buggyFunctions+", pair: ("+bugRep.functionPair[0]+", "
                        +bugRep.functionPair[1]+"), support: "+bugRep.support+", confidence: "+String.format("%.2f", bugRep.confidence)
                        +"%"); //print the output
            });

        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

    }

    private static Integer getIndividualSupport(String functionWErr) { //Given a function with error, calculates its individual support
        int counter=0;
        ArrayList<ArrayList<String>> valueList = new ArrayList<>(finalCallGraph.values());
        for(int i =  0;i<valueList.size();i++) {
            if(valueList.get(i).contains(functionWErr)){
                counter++;
            }
        }
        return counter;
    }

    private static void rec(String S1,int loop,int depth) {

        //Recursively adds the functions a function calls to update call graph to needed value. The number set in if determines it, 
        //and the number in the == must be 1 less than the < if. Greater number, greater depth of recursion.
        //System.out.println("Entered Rec");
        if(loop==depth-1) {
            ArrayList<String> toBeAdded = new ArrayList<>(newCallGraph.get(S1));

            callGraph.get(S1).forEach((t -> {
                toBeAdded.addAll(newCallGraph.get(t));
                newCallGraph.put(S1,toBeAdded);
            }));


            return;
        }else if(loop<depth){
            callGraph.get(S1).forEach((String t)->
            {
                if(finalCallGraph.containsKey(t)){
                    newCallGraph.put(t, finalCallGraph.get(t));
                } else {
                rec(t, loop+1,depth);
                }
            });

            ArrayList<String> toBeAdded = new ArrayList<>(newCallGraph.get(S1));

            callGraph.get(S1).forEach((t -> {
                toBeAdded.addAll(newCallGraph.get(t));
                newCallGraph.put(S1,toBeAdded);
            }));

            return;

        }



    }

    //calculate support
    private static Integer support(String[] functionPair) { //Calculates support for a pair
        int supportCtr = 0;
        ArrayList<String> keyList = new ArrayList<>(finalCallGraph.keySet());
        ArrayList<ArrayList<String>> valueList = new ArrayList<>(finalCallGraph.values());

        for(int i = 0;i<keyList.size();i++) {
            if(valueList.get(i).contains(functionPair[0])) {
                if(valueList.get(i).contains(functionPair[1])) {
                    supportCtr++;
                }else {
                    errors.add(new Error(new String[]{functionPair[0],functionPair[1]}, supportCtr, functionPair[0], keyList.get(i)));
                }
            }else if(valueList.get(i).contains(functionPair[1])) {
                errors.add(new Error(new String[]{functionPair[0],functionPair[1]}, supportCtr, functionPair[1], keyList.get(i)));
            }
        }
        return supportCtr;
    }



    //calculate confidence
    private static Double confidence(Integer supportPair, Integer supportIndividual) {
        if(supportIndividual==1)
            return 0.0;
        else
            return (supportPair*100.0)/(supportIndividual);
    }
}

class Error { //Error class storing the pair, the function with the error, the support and the buggy function.
    public String[] functionPair = new String[2];
    public Integer support = 0;
    public String functionWithError = "";
    public String buggyFunction = "";

    Error (String[] functionPairs, Integer support,
           String functionWithError, String buggyFunction) {
        this.functionPair = functionPairs;
        this.support = support;
        this.functionWithError = functionWithError;
        this.buggyFunction = buggyFunction;
    }
    public void updateError(String[] functionPairs, Integer support,
                            String functionWithError, String buggyFunction) {
        this.functionPair = functionPairs;
        this.support = support;
        this.functionWithError = functionWithError;
        this.buggyFunction = buggyFunction;
    }

}

class BugReport { //Bug report class storing the pair, the support, the function with error, the buggy function and the confidence of it being a bug.
    public String[] functionPair = new String[2];
    public Integer support = 0;
    public String functionWithError = "";
    public String buggyFunctions = "";
    public Double confidence = 0.0;

    BugReport(String[] functionPairs, Integer support,
              String functionWithError, String buggyFunctions, Double confidence) {
        this.functionPair = functionPairs;
        this.support = support;
        this.functionWithError = functionWithError;
        this.buggyFunctions = buggyFunctions;
        this.confidence = confidence;
    }

}
