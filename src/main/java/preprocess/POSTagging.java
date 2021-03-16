package preprocess;

import edu.stanford.nlp.ling.CoreAnnotations;


import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class POSTagging {
    private static StanfordCoreNLP pipeline;
    private ArrayList<String> bctSentenceArr;
    private int bctSentenceArrSize = 0;

    public int getBctSentenceArrSize() {
        return bctSentenceArrSize;
    }

    public void setBctSentenceArrSize(int bctSentenceArrSize) {
        this.bctSentenceArrSize = bctSentenceArrSize;
    }

    public ArrayList<String> getBctSentenceArr() {
        return bctSentenceArr;
    }

    public void setBctSentenceArr(ArrayList<String> bctSentenceArr) {
        this.bctSentenceArr = bctSentenceArr;
    }

    public void init() {
        Properties props = null;
        props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos");
        pipeline = new StanfordCoreNLP(props);

    }

    public List<String> lemmatize(String documentText) {
        List<String> lemmas = new LinkedList<>();
        Annotation document = new Annotation(documentText);
        pipeline.annotate(document);
        List<CoreMap> sentences = (List<CoreMap>) document.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                lemmas.add((String) token.get(CoreAnnotations.LemmaAnnotation.class));
                lemmas.add((String) token.get(CoreAnnotations.PartOfSpeechAnnotation.class));
            }
        }
        return lemmas;
    }

    public ArrayList<String> GetSplitRs(String documentText) {
        ArrayList<String> subSentences = null;
        ArrayList<String> bctSentenceArr = null;
        subSentences = new ArrayList<String>();
        List<Integer> lBctPos = null;
        List<Integer> rBctPos = null;
        int lBctnum = 0;
        int rBctnum = 0;
        lBctPos = getlBctArr(documentText);
        rBctPos = getrBctArr(documentText);
        lBctnum = lBctPos.size();
        rBctnum = rBctPos.size();
        if (lBctnum != rBctnum || lBctnum == 0 || rBctnum == 0) {
            Annotation document = new Annotation(documentText);
            pipeline.annotate(document);
            List<CoreMap> sentences = (List<CoreMap>) document.get(CoreAnnotations.SentencesAnnotation.class);
            for (CoreMap sentence : sentences) {
                subSentences.add(sentence.get(TextAnnotation.class));
            }
        } else if (lBctnum == rBctnum) {
            int rbctIndex = 0;
            int lbctIndex = 0;
            Stack stack = new Stack();
            ArrayList<int[]> posArr = new ArrayList<int[]>();
            bctSentenceArr = new ArrayList<String>();
            while (rbctIndex < lBctnum && lbctIndex < lBctnum) {
                if (lBctPos.get(lbctIndex) < rBctPos.get(rbctIndex)) {
                    stack.push(lBctPos.get(lbctIndex));
                    lbctIndex++;
                } else {
                    int lbctpos = -1;
                    if (!stack.empty()) {
                        lbctpos = (int) stack.pop();
                    }
                    if (lbctpos != -1 && stack.empty()) {
                        int[] posarr = new int[2];
                        posarr[0] = lbctpos;
                        posarr[1] = rBctPos.get(rbctIndex);
                        posArr.add(posarr);
                        String sentence = documentText.substring(posarr[0] + 1, posarr[1]);
                        if (sentence.length() > 0) {
                            bctSentenceArr.add(sentence);
                        }
                    }
                    rbctIndex++;
                }
            }
            if (lbctIndex == lBctnum && rbctIndex == rBctnum - 1) {
                int[] posarr = new int[2];
                posarr[0] = lBctPos.get(lbctIndex - 1);
                posarr[1] = rBctPos.get(rbctIndex);
                posArr.add(posarr);
                String sentence = documentText.substring(posarr[0] + 1, posarr[1]);
                if (sentence.length() > 0) {
                    bctSentenceArr.add(sentence);
                }
            }
            String midprocess = documentText;
            int bctSentenceNum = bctSentenceArr.size();
            for (int i = bctSentenceNum - 1; i >= 0; i--) {
                int[] posarr = (int[]) posArr.get(i);
                midprocess = midprocess.substring(0, posarr[0] + 1) + "SUBSENTENCE_INDEXOF-" + i + midprocess.substring(posarr[1], midprocess.length());
            }
            if (bctSentenceNum != 0) {
                setBctSentenceArr(bctSentenceArr);
                bctSentenceArrSize = bctSentenceNum;
            }
            Annotation document = new Annotation(midprocess);
            pipeline.annotate(document);
            List<CoreMap> sentences = (List<CoreMap>) document.get(CoreAnnotations.SentencesAnnotation.class);
            for (CoreMap midpr : sentences) {
                String midprstr = midpr.get(TextAnnotation.class);
                subSentences.add(midprstr);
            }
        }
        return subSentences;
    }

    public List<Integer> getlBctArr(String str) {
        String[] beforelBct = {"-", "\'", ":", ",", "_", "B", "=", "o"};
        List<Integer> posArr = new ArrayList<>();
        if (str.contains("(")) {
            int indexOf = str.indexOf("(", 0);
            while (true) {
                if (indexOf != -1) {
                    boolean isBct = true;
                    if (indexOf > 0) {
                        String before = str.substring(indexOf - 1, indexOf);
                        for (int i = 0; i < beforelBct.length; i++) {
                            if (before.equals(beforelBct[i])) {
                                isBct = false;
                                break;
                            }
                        }
                    }
                    if (isBct) {
                        posArr.add(indexOf);
                    }
                    indexOf = str.indexOf("(", indexOf + 1);
                } else {
                    break;
                }
            }
        }
        return posArr;
    }

    public List<Integer> getrBctArr(String str) {
        String[] afrBct = {";", "^", "o", "="};
        List<Integer> posArr = new ArrayList<>();
        if (str.contains(")")) {
            int indexOf = str.indexOf(")", 0);
            while (true) {
                if (indexOf != -1) {
                    boolean isBct = true;
                    if (indexOf + 2 < str.length()) {
                        String after = str.substring(indexOf + 1, indexOf + 2);
                        for (int i = 0; i < afrBct.length; i++) {
                            if (after.equals(afrBct[i])) {
                                isBct = false;
                                break;
                            }
                        }
                    }
                    if (isBct) posArr.add(indexOf);
                    indexOf = str.indexOf(")", indexOf + 1);
                } else {
                    break;
                }
            }
        }
        return posArr;
    }

    public String GetSingleSentenceLemmatize(String documentText) {
        String lemmas = null;
        Annotation document = new Annotation(documentText);
        pipeline.annotate(document);
        List<CoreMap> sentences = (List<CoreMap>) document.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class))
                lemmas = String.valueOf(lemmas) + (String) token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
        }
        System.out.println(lemmas);
        return lemmas;
    }

    public List<CoreMap> GetAllSentences(String documentText) {
        Annotation document = new Annotation(documentText);
        pipeline.annotate(document);
        List<CoreMap> sentences = (List<CoreMap>) document.get(CoreAnnotations.SentencesAnnotation.class);
        return sentences;
    }

    public String getWordLemmatize(String word) {
        String lemmaWord = "";
        CoreLabel token = new CoreLabel();
        token.setValue(word);
        lemmaWord = (String) token.get(CoreAnnotations.LemmaAnnotation.class);
        Annotation tokenAnnotation = new Annotation(word);
        pipeline.annotate(tokenAnnotation);
        List<CoreMap> list = (List<CoreMap>) tokenAnnotation.get(CoreAnnotations.SentencesAnnotation.class);
        lemmaWord = (String) ((CoreLabel) ((List<CoreLabel>) ((CoreMap) list
                .get(0)).get(CoreAnnotations.TokensAnnotation.class))
                .get(0)).get(CoreAnnotations.LemmaAnnotation.class);
        return lemmaWord;
    }

    private boolean tagStart = false;
    private int istart = 0;
    private String[] adv = {"RB", "JJ"};
    private ArrayList<String> words = new ArrayList<String>();
    private ArrayList<String> tags = new ArrayList<String>();
    private int arrsize = 0;

    public void initText(String str) {
        if (!tagStart) {
            Annotation document = new Annotation(str);
            pipeline.annotate(document);
            List<CoreMap> sentences = (List<CoreMap>) document.get(CoreAnnotations.SentencesAnnotation.class);
            for (CoreMap sentence : sentences) {
                for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                    words.add(token.get(TextAnnotation.class).toLowerCase());
                    tags.add(token.get(PartOfSpeechAnnotation.class));
                    arrsize++;
                }
            }
            tagStart = true;
        }
    }

    public int turnLyingBycontext(String str, String word) {
        initText(str);
        for (int i = istart; i < arrsize; ++i) {
            if (words.get(i).equals(word)) {
                istart = i;
                if (getAftertag(1).equals("RB")) {
                    return 0;
                }
                if (getAftertag(1).equals("IN") && !getAfterwrd(1).equals("to")) {
                    return 0;
                }
                if (getAftertag(2).equals("IN") && !getAfterwrd(2).equals("to")) {
                    return 0;
                }
                break;
            }
        }
        return -3;
    }

    public int turnLikeBycontext(String str, String word) {
        initText(str);
        for (int i = istart; i < arrsize; ++i) {
            if (words.get(i).equals(word)) {
                istart = i;
                if (isVerb(tags.get(i))) {
                    return 1;
                } else {
                    int start = Math.max(1, i - 3);
                    for (int j = i - 1; j >= start; j--) {
                        if (words.get(j).equals("i")) {
                            return 1;
                        }
                    }
                    break;
                }
            }
        }
        return 0;
    }

    public int turnMissBycontext(String str, boolean flag) {
        if (flag) {
            initText(str);
            for (int i = istart; i < arrsize; ++i) {
                if (words.get(i).equals("ill")) {
                    istart = i;
                    if (tags.get(i).equals("JJ")) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            }
            return -1;
        } else {
            return -1;
        }
    }

    public int turnPrettyBycontext(String str, String word) {
        String tag = readWordPos(str, word);
        if (tag.equals("noResult")) return 0;
        String aftwrdtag = getAftertag(1);
        if (isAdv(aftwrdtag) || isAdj(aftwrdtag)) {
            return 0;
        }
        return 2;
    }

    public int turnVerbBycontext(String str, String word, int senti) {
        String tag = readWordPos(str, word);
        if (isVerb(tag) && !tag.equals("VBG")) {
            return senti;
        }
        return 0;
    }

    public String readWordPos(String str, String word) {
        initText(str);
        if (arrsize == 0) {
            return "noResult";
        }
        for (int i = istart; i < arrsize; ++i) {
            if (words.get(i).equals(word)) {
                istart = i;
                return tags.get(i);
            }
        }
        return "noResult";
    }

    public boolean isAdv(String tag) {
        return isHave(tag, adv);
    }

    public boolean isHave(String word, String[] possTag) {
        for (int i = 0; i < possTag.length; i++) {
            if (word.equals(possTag[i])) {
                return true;
            }
        }
        return false;
    }

    public boolean isAdj(String tag) {
        if (tag.length() > 2) {
            tag = tag.substring(0, 2);
        }
        return tag.equals("JJ");
    }

    public boolean isConj(String word) {
        return word.equals("to") || word.equals("for");
    }

    public boolean isVerb(String tag) {
        if (tag.length() > 2) {
            tag = tag.substring(0, 2);
        }
        return tag.equals("VB");
    }

    public boolean isBeVerb(String word) {
        String[] beVerb = {"is", "are", "was", "were", "am", "be", "being", "been", "isn't", "aren't", "wasn't", "weren't"};
        return isHave(word, beVerb);
    }

    private boolean IsBeVerbAbb(String word, String tag) {
        if (tag.equals("NNP")) {
            return false;
        }
        String[] beVerb = {"'m", "'s", "'re"};
        return isHave(word, beVerb);
    }

    private boolean IsBeVerb(String word, String beforedWordTag) {
        if (beforedWordTag.equals("WDT") || beforedWordTag.equals("WP") || beforedWordTag.equals("WRB")) {
            return false;
        }
        String[] beVerb = {"'m", "'s", "'re"};
        if (isHave(word, beVerb) && !beforedWordTag.equals("NNP")) {
            return true;
        }
        return isBeVerb(word);
    }

    private boolean IsCopula(String word) {
        String[] beVerb = {"look", "seem"};
        return isContain(word, beVerb);
    }

    public boolean isContain(String word, String[] possWrd) {
        for (int i = 0; i < possWrd.length; i++) {
            if (word.indexOf(possWrd[i]) != -1) {
                return true;
            }
        }
        return false;
    }

    public boolean isNoun(String tag) {
        if (tag.length() > 2) {
            tag = tag.substring(0, 2);
        }
        return tag.equals("NN");
    }

    public boolean isPronoun(String word) {
        String[] pronoun = {"it", "this", "that"};
        return isHave(word, pronoun);
    }

    public boolean isA(String word) {
        String[] a = {"a", "an", "n"};
        return isHave(word, a);
    }

    public boolean isGet(String word) {
        if (word.length() > 3) {
            word = word.substring(0, 3);
        }
        return word.equals("get") || word.equals("got");
    }

    public boolean isLadian(String str, String word) {
        if (arrsize == 1) {
            return false;
        }
        String tag = readWordPos(str, word);
        String beforetag;
        String beforewrd;
        String afterwrd;
        for (int i = 1; i <= 3; i++) {
            beforewrd = getBeforewrd(i);
            if (isGet(beforewrd)) {
                return true;
            }
        }
        if (tag.equals("NN") || isAdj(tag)) {
            for (int i = 1; i <= 2; i++) {
                beforewrd = getBeforewrd(i);
                if (IsBeVerb(beforewrd, getBeforetag(i + 1)) && istart - i != 0) {
                    return true;
                }
            }
        }
        beforewrd = getBeforewrd(1);
        beforetag = getBeforetag(1);
        afterwrd = getAfterwrd(1);
        if (beforewrd.equals("my")) {
            return true;
        }
        if (tag.equals("UH")) {
            return true;
        } else if (tag.equals("RB")) {
            return true;
        } else if (tag.equals("NN")) {
            if (isA(beforewrd) || beforewrd.equals("me")) {
                return true;
            } else {
                beforewrd = getBeforewrd(2) + beforewrd;
                if (beforewrd.equals("sortsof") || beforewrd.equals("sortof")) {
                    return true;
                }
            }
            if (beforetag.equals("JJ")) {
                return true;
            }
            if (isBeVerb(afterwrd)) {
                return true;
            }
        } else if (isAdj(tag)) {
            if (beforetag.length() > 2) {
                beforetag = beforetag.substring(0, 2);
            }
            if (isAdv(beforetag) || beforetag.equals("DT") || beforetag.equals("CC")) {
                return true;
            }
            if (beforewrd.equals("how") || beforewrd.equals("me") || isA(beforewrd) || IsCopula(beforewrd)) {
                return true;
            } else {
                beforewrd = getBeforewrd(2) + beforewrd;
                if (beforewrd.equals("sortsof") || beforewrd.equals("sortof")) {
                    return true;
                }
            }
            if (afterwrd.equals("enough")) {
                return true;
            }
        } else if (isVerb(tag)) {
            if (isPronoun(beforewrd)) {
                return true;
            }
            if (afterwrd.equals("me")) {
                return true;
            }
        }
        return false;
    }

    public String getBeforetag(int i) {
        if (istart >= i) {
            return tags.get(istart - i);
        } else {
            return "noResult";
        }
    }

    public String getBeforewrd(int i) {
        if (istart >= i) {
            return words.get(istart - i).toLowerCase();
        } else {
            return "noResult";
        }
    }

    public String getAftertag(int i) {
        if (istart < arrsize - i) {
            return tags.get(istart + i);
        } else {
            return "noResult";
        }
    }

    public String getAfterwrd(int i) {
        if (istart < arrsize - i) {
            return words.get(istart + i);
        } else {
            return "noResult";
        }
    }

    public String getTag(int i) {
        if (i < arrsize) {
            return tags.get(i);
        } else {
            return "noResult";
        }
    }

    public String getWord(int i) {
        if (i < arrsize) {
            return tags.get(i);
        } else {
            return "noResult";
        }
    }

    public String readPOS(String str, String word) {
        initText(str);
        if (arrsize == 0) {
            return "noResult";
        }
        for (int i = 0; i < arrsize; ++i) {
            if (words.get(i).equals(word)) {
                return tags.get(i);
            }
        }
        return "noResult";
    }
}