import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class src {
    //Needed variables for analysis. callGraph respresents the entire call graph data is read into.
    //Bug reports are the final reports to be printed, errors store needed information to create the bug reports.
    //Individual supports store each functions support value for support value for confidence calculation.
    static HashMap<String, ArrayList<String>> callGraph = new HashMap<>();
    static ArrayList<BugReport> bugReports = new ArrayList<>();
    static ArrayList<Error> errors = new ArrayList<>();
    static HashMap<String, Integer> individualSupports = new HashMap<>();
    static HashMap<String, ArrayList<String>> newCallGraph = new HashMap<>();

    public static void main(String[] args) {
        //functionPairsWithSupport stores the supports of pairs to calculate confidence.
        HashMap<String, Integer> functionPairsWithSupport = new HashMap<>();
        Integer supportHolder = 3;
        Integer confidenceHolder = 65;
        if(args.length>1) {
        supportHolder = Integer.parseInt(args[1]);
        confidenceHolder = Integer.parseInt(args[2]);
        }
        final Integer supportRequirement = supportHolder;
        final Integer confidenceRequirement = confidenceHolder;
       
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

            callGraph.forEach((t,u) -> 
                individualSupports.put(t, getIndividualSupport(t)) //updates all individual support values
            );

            ArrayList<String> allFunctionsList = new ArrayList<>(individualSupports.entrySet().stream().filter(x -> x.getValue() >= supportRequirement)
                    .map(Map.Entry::getKey).collect(Collectors.toList()));
                    
            allFunctionsList.sort(Comparator.naturalOrder());
            
                    for (int i = 0; i < allFunctionsList.size() - 1; i++) {
                for (int j = i + 1; j < allFunctionsList.size(); j++) {
                    // call the support function to find the support and fill up the error_in field
                    int support = support(new String[]{allFunctionsList.get(i), allFunctionsList.get(j)});
                    functionPairsWithSupport.put(allFunctionsList.get(i)+allFunctionsList.get(j), support);
                }
            }
            
            for(int i = 0;i<errors.size();i++) { //go through all errors
                String buggedFunc = errors.get(i).buggyFunction;
                if( buggedFunc != "") {
                    String[] pair = (errors.get(i).functionPair);
                    String pairString = pair[0]+pair[1];
                    String functionWErr = errors.get(i).functionWithError;
                    if(pair!=null && functionPairsWithSupport.get(pairString)!=null && functionWErr!= null){ //check if values we have are non null

                        int supportPair = functionPairsWithSupport.get(pairString);
                        if(individualSupports.get(functionWErr) != null){
                            Double confidence = confidence(supportPair, individualSupports.get(functionWErr));
                            if(confidence>= confidenceRequirement && supportPair>= supportRequirement) {
                                bugReports.add(new BugReport(pair, supportPair, functionWErr, buggedFunc, confidence)); //Get support and calculate confidence, then add to bug reports
                            }
                        }
                        
                    }
                }
            }
            bugReports.forEach((BugReport bugRep)-> {
                System.out.println("bug: "+bugRep.functionWithError+" in "+bugRep.buggyFunctions+", pair: ("+bugRep.functionPair[0]+", "
                        +bugRep.functionPair[1]+"), support: "+bugRep.support+", confidence: "+String.format("%.2f", bugRep.confidence)
                        +"%"); // Print all the bug reports.
            });

        } catch (FileNotFoundException e) { //Catch error if call graph file cannot be read
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

    }

    private static Integer getIndividualSupport(String functionWErr) { //Given a function with error, calculates its individual support
        int counter=0;
        ArrayList<ArrayList<String>> valueList = new ArrayList<>(callGraph.values());
        for(int i =  0;i<valueList.size();i++) {
            if(valueList.get(i).contains(functionWErr)){
                counter++;
            }
        }
        return counter;
    }

    //calculate support
    private static Integer support(String[] functionPair) { //Calculates support for a pair
        int supportCtr = 0;
        ArrayList<String> keyList = new ArrayList<>(callGraph.keySet());
        ArrayList<ArrayList<String>> valueList = new ArrayList<>(callGraph.values());

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
    private static Double confidence(Integer supportPair, Integer supportIndividual) { //Formula for confidence
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
