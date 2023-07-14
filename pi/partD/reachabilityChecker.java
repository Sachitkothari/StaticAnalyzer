import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.io.*;

public class reachabilityChecker {
    static HashMap<String, ArrayList<String>> callGraph = new HashMap<>(); //Hashmap representing the call graph
    static HashSet<String> allFuncSet = new HashSet<>();   //A set of every function                                                                                                                                                                                                                                                                  
    public static void main(String[] args) { 
                try {
            File myObj = new File("call_graph.txt");
            Scanner myReader = new Scanner(myObj);
            String firstNode = "";
            //Read call graph file
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
                    String leftFunction = firstNode.substring(firstOccurance+1,lastOccurance);
                    firstNode = myReader.nextLine();
                    ArrayList<String> str = new ArrayList<String>();
                    callGraph.put(leftFunction,str); //update call graph
                    allFuncSet.add(leftFunction); //Add function to set
                    while(firstNode.contains("calls function")||firstNode.contains("calls external node")) {
                        if(firstNode.contains("calls external node")) {
                            firstNode = myReader.nextLine();
                            continue;
                        }
                        firstOccurance = firstNode.indexOf("'");
                        lastOccurance = firstNode.lastIndexOf("'");
                        String rightFunction = firstNode.substring(firstOccurance+1,lastOccurance);
                        callGraph.get(leftFunction).add(rightFunction);
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
            callGraph.forEach(((t, u) ->{ //Goes over every function in call graph and removes any function called from set.
                u.forEach((String func)-> {
                    if(allFuncSet.contains(func)){
                        allFuncSet.remove(func);
                    }
                });
            } ));
            PrintStream o = new PrintStream(new File("out.txt"));
            PrintStream console = System.out;
            System.setOut(o);
            allFuncSet.remove("main");
            System.out.println(allFuncSet); //Remaining functions in set are unreachable and printed
            System.out.println(allFuncSet.size()); //Number of unreachable functions printed
            
    }  catch (FileNotFoundException e) {//Case for error in reading call graph file
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
}
}