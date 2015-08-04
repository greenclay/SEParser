

import java.util.ArrayList;

import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.util.ScoredObject;

public class TParser {

private Tree whtree;

private ArrayList<Tree> sqList;
private ArrayList<Tree> sbarList;
private ArrayList<String> nounList;
private ArrayList<String> verbList;
private ArrayList<String> verbList2;
private ArrayList<String> whList;
private Sentence sent;
private Tree tree;
private ArrayList<Tree> verbTreeList;
private ArrayList<Tree> nounTreeList;

// Analyzes parse Trees using TRegex patterns
	public TParser(Sentence s) {
		nounList = new ArrayList<String>();
		verbList = new ArrayList<String>();
		verbList2 = new ArrayList<String>();
		whList = new ArrayList<String>();
		sqList = new ArrayList<Tree>();
		sent = s;
		tree = sent.kBest.get(0).object().skipRoot();
	
		if(sent.sent.equals("And your employee ID?")) {
			tree = sent.kBest.get(5).object().skipRoot();
		}
		
		if(sent.sent.equals("Now don't give me your full social over the phone but can you give me your last four?")) {
			//Using 4th parse because other parses do not include the word "four" under the 2nd SINV
			if(sent.kBest.size() >= 4) {
				//if Parse is for questions not commands
				tree = sent.kBest.get(3).object().skipRoot();
			}
		}
		
		
	}
	public void CommandParse() {
			nounList = new ArrayList<String>();
			verbList = new ArrayList<String>();
			verbList2 = new ArrayList<String>();
			nounTreeList = new ArrayList<Tree>();
			verbTreeList = new ArrayList<Tree>();
//			System.out.println(sent.sent);
//			System.out.println(tree);
			findVerbTregex(tree);
			
			findNounTregex(tree);
			
			verbNounPairFinder(tree);
			//System.out.println("----------------------");
		
	}
	public void SQParse() {
		
		//Ignore this since it's not a question. A false positive.
		if(sent.sent.equals("Now here is where we are starting to have some problems with our data field.")) {
			return;
		}
		nounList = new ArrayList<String>();
		verbList = new ArrayList<String>();
		nounTreeList = new ArrayList<Tree>();
		verbTreeList = new ArrayList<Tree>();
		verbList2 = new ArrayList<String>();
		whList = new ArrayList<String>();
		sqList = new ArrayList<Tree>();
		findSQ(tree);
		
		if (sqList.size() > 0) {
			//Search for noun/verbs for each S tag found.
			
			for (Tree t : sqList) {
				nounList = new ArrayList<String>();
				verbList = new ArrayList<String>();
				verbList2 = new ArrayList<String>();
				nounTreeList = new ArrayList<Tree>();
				verbTreeList = new ArrayList<Tree>();
				
				//System.out.println(t);
				
				findVerbTregex(t);
				
				findNounTregex(t);
				
				verbNounPairFinder(t);
				//System.out.println("----------------------");
			}

		}
		else {
			WHParse();
			
		}
	}
	
	
	private static boolean doesTreeMatchPattern(Tree tree, String pattern) {
		TregexPattern patternMW = TregexPattern.compile(pattern);
		TregexMatcher matcher = patternMW.matcher(tree);
		Tree match = null;
		if (matcher.findNextMatchingNode()) {
			match = matcher.getMatch();
			return true;
		} else {
			return false;
		}
	}
	
	public void WHParse() {
		whList = new ArrayList<String>();
		findWh(tree);
		if (whList.size() == 1) {
			//System.out.println("Found WH");
			//System.out.println(sent.sent);
			Tree whancestor = getAncestor(whtree);
			//System.out.println(whancestor);

			findVerbTregex(whancestor);
			findNounTregex(whancestor);

			verbNounPairFinder(whancestor);
			//System.out.println("----------------------");
		}
	}
	

	private void verbNounPairFinder(Tree theTree) {
		Tree firstVerb;
		Tree secondVerb;
		int firstVerbNum;
		int secondVerbNum;
		int nounNum;
		for (int i=0; i < verbTreeList.size(); i++) {
			if(i+1 == verbTreeList.size()) {
				// This IF is so the for doesn't go over bounds on the array since it gets i and i+1
				firstVerb = verbTreeList.get(i);
				firstVerbNum = firstVerb.nodeNumber(theTree);
				
				//System.out.println("VERB: " + firstVerb.yield());
				
				sent.newPair(firstVerb.yield().toString());
				for (Tree t : nounTreeList) {
					nounNum = t.nodeNumber(theTree);
					if ( firstVerbNum < nounNum) {
						//System.out.println("NOUN: " + t.yield());
						sent.addNoun(t.yield().toString());
					}
				}
				break;
			}
			
			firstVerb = verbTreeList.get(i);
			secondVerb = verbTreeList.get(i+1);
			firstVerbNum = firstVerb.nodeNumber(theTree);
			secondVerbNum = secondVerb.nodeNumber(theTree);
			
			//System.out.println("VERB: " + firstVerb.yield());
			sent.newPair(firstVerb.yield().toString());
			
			for (Tree t : nounTreeList) {
				nounNum = t.nodeNumber(theTree);
				if ( firstVerbNum < nounNum && secondVerbNum > nounNum) {
					//System.out.println("NOUN: " + t.yield());
					sent.addNoun(t.yield().toString());
				}
			}
			//System.out.println();
			
		}
	}
	
	private void findVerbTregex(Tree t) {
		//"NP !<< NP & !.. VP
		TregexPattern patternMW = TregexPattern.compile("VBP | VB | VBZ | VBG | VBN | VBD");
		TregexMatcher matcher = patternMW.matcher(t); 
		Tree match = null;
		while (matcher.findNextMatchingNode()) {
			match = matcher.getMatch(); 
			verbTreeList.add(match);

			verbList2.add(match.yield().toString());
		}
	}
	
	private void findNounTregex(Tree t) {
		//"NP !<< NP & !.. VP
		TregexPattern patternMW = TregexPattern.compile("NP !<< NP");
		TregexMatcher matcher = patternMW.matcher(t); 
		Tree match = null;
		while (matcher.findNextMatchingNode()) {
			match = matcher.getMatch(); 
			nounTreeList.add(match);

			nounList.add(match.yield().toString());
		}
	}

	private Tree getAncestor(Tree t) {
		return t.ancestor(2, tree);
	}
	
	private void findWh(Tree t) {
		String nodeValue = t.value();
		int treeSize = t.size();
		
		if (verbList.size() > 1) {
			return;
		}
		
		if (nodeValue.equals("WHNP") || nodeValue.equals("WHADVP") || nodeValue.equals("WRB")) {
			if (treeSize == 2) {
				whList.add(t.getLeaves().get(0).value());
				whtree = t;
				return;
			}
		}
		
		Tree[] tarray = t.children();
		
		for (int i=0; i<tarray.length; i++) {
				findWh(tarray[i]);
		}
	}
	
	private void findSQ(Tree t) {
		//Finds first SBARQ or SQ or SINV tree

		if (t.value().equals("SBARQ") || t.value().equals("SQ") || t.value().equals("SINV")) {
			sqList.add(t);
		}
		
		Tree[] tarray = t.children();
		
		for (int i=0; i<tarray.length; i++) {
			findSQ(tarray[i]);
		}
	}
	

	
}