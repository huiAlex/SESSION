package preprocess;

import edu.stanford.nlp.util.CoreMap;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtility {
    private String regExForCamelCaseStartWithCapLetter = "(^[A-Z][a-z0-9]+[A-Z]$)|(^[A-Z][a-z0-9]+([A-Z][a-z0-9]+)+$)|(^[A-Z][a-z0-9]+([A-Z][a-z0-9]+)+[A-Z]$)";

    private String regExForCamelCaseStartWithSmallLetter = "^[a-z]+([A-Z][a-z0-9(]+)+";

    private String regExForMultipleCapitalLetter = "(?!^.*[A-Z]{2,}.*$)^[A-Za-z]*$";

    private String regExForURL = "\\(?\\b(http[s ]://|www[.]| ftp)[-A-Za-z0-9+&amp;@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&amp;@#/%=~_()|]";

    private String regExForStringWithSlash = ".*[/\\\\].*";

    private String regExForThreeConsecutiveLetter = "\\b([a-zA-Z0-9])\\1\\1+\\b";

    private Pattern pattern;

    private Matcher matcher;

    private int serchRangeofPersonalpronoun = 3;

    private int forwardSearchRange;

    private int backwardSearchRange;

    private String processedWord;

    boolean SpellingCorrectionConfig = false;

    private POSTagging objPOSTagging = new POSTagging();

    private static ArrayList<String> listOfEmoticon;

    private ArrayList<String> bctSentenceArr;

    private int bctArrSize;

    public static void setListOfEmoticon(ArrayList<String> listOfEmo) {
        if (listOfEmoticon == null) {
            FileUtility.listOfEmoticon = listOfEmo;
        }
    }

    public int getBctArrSize() {
        return bctArrSize;
    }

    public void setBctArrSize(int bctArrSize) {
        this.bctArrSize = bctArrSize;
    }

    public ArrayList<String> getBctSentenceArr() {
        return bctSentenceArr;
    }

    public void setBctSentenceArr(ArrayList<String> bctSentenceArr) {
        this.bctSentenceArr = bctSentenceArr;
    }

    public void setSpellingCorrectionConfig(boolean paramSpellingCorrectionConfig) {
        this.SpellingCorrectionConfig = paramSpellingCorrectionConfig;
    }

    private String dealWithExclamation(String str) {
        int pos = str.indexOf("!");
        while (pos != -1) {
            if (pos < str.length() - 1) {
                char tail = str.charAt(pos + 1);
                boolean isLetter = (tail >= 'a' && tail <= 'z') || (tail >= 'A' && tail <= 'Z');
                if (isLetter) {
                    str = str.substring(0, pos) + "! " + str.substring(pos + 1, str.length());
                }
            }
            pos = str.indexOf("!", pos + 1);
        }
        return str;
    }

    private String breviary(String Str) {
        ArrayList<String> replace1 = new ArrayList<String>();
        ArrayList<String> replace2 = new ArrayList<String>();
        replace1.add("t ");
        replace1.add("d ");
        replace1.add("m ");
        replace1.add("s ");
        replace2.add("ve ");
        replace2.add("ll ");
        int pos = Str.indexOf("' ");
        String tail1 = null;
        String tail2 = null;
        while (pos != -1) {
            if (pos < Str.length() - 5) {
                tail1 = Str.substring(pos + 2, pos + 4);
                tail2 = Str.substring(pos + 2, pos + 5);
            } else {
                break;
            }
            if (replace1.contains(tail1)) {
                Str = Str.substring(0, pos) + "'" + Str.substring(pos + 2, Str.length());
            } else if (replace2.contains(tail2)) {
                Str = Str.substring(0, pos) + "'" + Str.substring(pos + 2, Str.length());
            }
            pos = Str.indexOf("' ", pos + 5);
        }
        pos = Str.indexOf(" '");
        while (pos != -1) {
            if (pos < Str.length() - 5) {
                tail1 = Str.substring(pos + 2, pos + 4);
                tail2 = Str.substring(pos + 2, pos + 5);
            } else {
                break;
            }
            if (replace1.contains(tail1)) {
                Str = Str.substring(0, pos) + "'" + Str.substring(pos + 2, Str.length());
            } else if (replace2.contains(tail2)) {
                Str = Str.substring(0, pos) + "'" + Str.substring(pos + 2, Str.length());
            }
            pos = Str.indexOf(" '", pos + 5);
        }
        return Str;
    }

    public String processedBySym(String sentence, boolean iFiltered) {
        if (iFiltered) {
            sentence = dealWithExclamation(breviary(sentence));
            if (sentence.indexOf("!=") != -1) {
                sentence = sentence.replaceAll("!=", "");
            }
            if (sentence.indexOf("<%") != -1) {
                sentence = sentence.replaceAll("<%(.*?)%>", "");
            }
            if (sentence.indexOf("{") != -1) {
                sentence = sentence.replaceAll("\\{(.*?)\\}", "");
            }
            if (sentence.indexOf("[") != -1) {
                sentence = sentence.replaceAll("(\\[(.*?)])", "[]");
            }
            if (sentence.indexOf("``") != -1) {
                sentence = sentence.replaceAll("``(.*?)``", "");
                sentence = sentence.replaceAll("``(.*?)''", "");
            }
            if (sentence.indexOf("\"") != -1) {
                sentence = sentence.replaceAll("\"(.*?)\"", "''");
            }
        }
        return sentence;
    }

    public ArrayList<String> GetProcessedArray(String rawLine, String systemNamestoIgnore, boolean iFiltered, Map<String, String> modifiedTermsMap) {
        rawLine = processedBySym(rawLine, iFiltered);
        ArrayList<String> processedArray = new ArrayList<String>();
        List<String> AllSentences = this.getObjPOSTagging().GetSplitRs(rawLine);
        for (String sentence : AllSentences) {
            processedArray.add(GetFilteredLine(sentence, modifiedTermsMap, iFiltered));
        }
        int bctSentenceArrSize = this.getObjPOSTagging().getBctSentenceArrSize();
        if (bctSentenceArrSize != 0) {
            ArrayList<String> bctSentenceArr = this.getObjPOSTagging().getBctSentenceArr();
            for (int i = 0; i < bctSentenceArrSize; i++) {
                String bctsentence = bctSentenceArr.get(i);
                bctsentence = GetFilteredLine(bctsentence, modifiedTermsMap, iFiltered);
                bctSentenceArr.set(i, bctsentence);
            }
            setBctSentenceArr(bctSentenceArr);
            setBctArrSize(bctSentenceArrSize);
        }
        return processedArray;
    }

    public String GetFilteredLine(String sentence, Map<String, String> modifiedTermsMap, boolean iFiltered) {
        String pcdlinesen = "";
        String previousWord = "";
        String[] words = sentence.trim().split(" ");
        int j;
        int i;
        String[] arrayOfString1;
        for (i = (arrayOfString1 = words).length, j = 0; j < i; j++) {
            String word = arrayOfString1[j];
            if (iFiltered) {
                if (word.length() == 0) continue;
                if (word.indexOf("non-") != -1 || word.indexOf("no-") != -1) continue;
                if (word.contains("SUBSENTENCE_INDEXOF")) {
                    pcdlinesen += " " + word;
                    continue;
                }
                if (word.charAt(0) == '@' || word.indexOf("_") != -1) {
                    pcdlinesen += " " + getPunc(word);
                    continue;
                }

                this.pattern = Pattern.compile(this.regExForURL);
                this.matcher = this.pattern.matcher(word);
                if (this.matcher.matches()) {
                    continue;
                }

                this.pattern = Pattern.compile(this.regExForStringWithSlash);
                this.matcher = this.pattern.matcher(word);
                if (this.matcher.matches()) {
                    continue;
                }

                this.pattern = Pattern.compile(this.regExForCamelCaseStartWithSmallLetter);
                this.matcher = this.pattern.matcher(word);
                if (this.matcher.matches()) {
                    continue;
                }

                this.pattern = Pattern.compile(this.regExForCamelCaseStartWithCapLetter);
                this.matcher = this.pattern.matcher(word);
                if (this.matcher.matches()) {
                    continue;
                }

                if ((word.split("\\.")).length > 2) {
                    if (word.split("\\.")[0].length() >= 2 && word.split("\\.")[1].length() > 2) {
                        continue;
                    }
                }
                if (word.equals("=") || word.equals("<=") || word.equals(">=")) {
                    previousWord = "comparisonMark";
                    continue;
                }
                if (word.contains("=") || word.contains("->") || word.contains("<=>") || word.contains("=>") || word.contains(">=") || word.contains("<=") || word.contains("@") || word.contains("<") || word.contains("/>") || word.contains("[") || word.contains("]") || word.contains("<>") || word.contains("||") || word.contains("&")) {
                    continue;
                }
                if (word.contains(":") && (word.split(":")).length >= 2 && word.split(":")[0].length() > 2 && word.split(":")[1].length() > 2) {
                    continue;
                }
                if ((!word.contains("ri8") && !word.contains("gr8") && word.contains("1")) || word.contains("2") || word.contains("3") || word.contains("4") || word.contains("5") || word.contains("6") || word.contains("7") || word.contains("9")) {
                    continue;
                }
                if (!word.contains(".") && word.startsWith("(") && word.endsWith(")")) {
                    word = word.replace("(", "");
                    word = word.replace(")", "");
                }
                if (word.contains("()")) continue;
                if (j < i - 1 && arrayOfString1[j + 1].equals("()")) continue;
                if (previousWord.equals("hi") || previousWord.equals("hello") || previousWord.equals("hellow") || previousWord.equals("dear") || previousWord.equals("@")) {
                    word = getPurePersoanl(word);
                    previousWord = word;
                }
                if (previousWord.equals("comparisonMark")) {
                    continue;
                }
                if (!word.isEmpty()) {
                    previousWord = word.toLowerCase();
                }
            }
            pcdlinesen += " " + word;
            continue;
        }
        if (modifiedTermsMap != null)
            pcdlinesen = GetProcessedLineAfterSearchingPersoanlPronoun(pcdlinesen, modifiedTermsMap);
        return pcdlinesen;
    }

    private String getPurePersoanl(String text) {
        String punc = text.replaceAll("[a-zA-Z0-9']+", "");
        String[] arr = text.split("[^a-zA-Z0-9']+");
        String rs = "";
        for (int i = 1; i < arr.length; i++) {
            rs += arr[i];
        }
        return punc + rs;
    }

    private String getPunc(String text) {
        String punc = text.replaceAll("[^,]+", "");
        return punc;
    }

    //  使用别的processed的时候注意检查sentence.toString()，要替换成sentence.get(TextAnnotation.class)；
    private String GetProcessedLineAfterSearchingPersoanlPronoun(String sentence, Map<String, String> modifiedTermsMap) {
        boolean isPersoanalPronounFoundAsneighbour = false;
        String finalProcessdLine = "";
        if (sentence.contains(" ")) {
            String[] wordsInprocessedLine = sentence.split(" ");
            for (int i = 0; i < wordsInprocessedLine.length; i++) {
                this.processedWord = wordsInprocessedLine[i];
                String word = this.processedWord.split(" ")[0].toLowerCase().replaceAll("[^a-z]", "");
                if (modifiedTermsMap.containsKey(word)) {
                    if (i - this.serchRangeofPersonalpronoun - 1 < 0) {
                        this.backwardSearchRange = 0;
                    } else {
                        this.backwardSearchRange = i - this.serchRangeofPersonalpronoun - 1;
                    }
                    if (i + this.serchRangeofPersonalpronoun > wordsInprocessedLine.length) {
                        this.forwardSearchRange = wordsInprocessedLine.length;
                    } else {
                        this.forwardSearchRange = i + this.serchRangeofPersonalpronoun + 1;
                    }
                    try {
                        isPersoanalPronounFoundAsneighbour = IsPersonalPronounNeighbour(sentence, this.forwardSearchRange, this.backwardSearchRange, i);
                    } catch (Exception ex) {
                        ex.getStackTrace();
                    }
                    if (isPersoanalPronounFoundAsneighbour) {
                        String punc = this.processedWord.split(" ")[0].toLowerCase().replaceAll("[a-z]", "");
                        finalProcessdLine = String.valueOf(finalProcessdLine) + (String) modifiedTermsMap.get(word) + punc + " ";

                    } else {
                        finalProcessdLine = String.valueOf(finalProcessdLine) + wordsInprocessedLine[i] + " ";
                    }
                } else {
                    finalProcessdLine = String.valueOf(finalProcessdLine) + wordsInprocessedLine[i] + " ";
                }
            }
        }
        return finalProcessdLine;
    }

    private String GetProcessedLineAfterSearchingNeutralizer(String processedLine, ArrayList<String> allEmotionTerms) {
        boolean isNeutralizerFoundAsNeighbour = false;
        String finalProcessdLine = "";
        processedLine = processedLine.toLowerCase().trim();
        List<CoreMap> AllSentences = this.getObjPOSTagging().GetAllSentences(processedLine);
        for (CoreMap sentence : AllSentences) {
            if (sentence.toString().contains(" ")) {
                String[] wordsInprocessedLine = sentence.toString().split(" ");
                for (int i = 0; i < wordsInprocessedLine.length; i++) {
                    this.processedWord = wordsInprocessedLine[i];
                    if (allEmotionTerms.contains(this.processedWord.split(" ")[0])) {
                        if (i - this.serchRangeofPersonalpronoun - 1 < 0) {
                            this.backwardSearchRange = 0;
                        } else {
                            this.backwardSearchRange = i - this.serchRangeofPersonalpronoun - 1;
                        }
                        if (i + this.serchRangeofPersonalpronoun > wordsInprocessedLine.length) {
                            this.forwardSearchRange = wordsInprocessedLine.length;
                        } else {
                            this.forwardSearchRange = i + this.serchRangeofPersonalpronoun + 1;
                        }
                        try {
                            isNeutralizerFoundAsNeighbour = IsNeutralizerNeighbour(sentence.toString(), this.backwardSearchRange, i);
                        } catch (Exception ex) {
                            ex.getStackTrace();
                        }
                        if (isNeutralizerFoundAsNeighbour) {
                            finalProcessdLine = String.valueOf(finalProcessdLine) + " " + "none" + " ";
                        } else {
                            finalProcessdLine = String.valueOf(finalProcessdLine) + wordsInprocessedLine[i] + " ";
                        }
                    } else {
                        finalProcessdLine = String.valueOf(finalProcessdLine) + wordsInprocessedLine[i] + " ";
                    }
                }
                continue;
            }
            finalProcessdLine = String.valueOf(finalProcessdLine) + " " + sentence.toString();
        }
        return finalProcessdLine.toLowerCase();
    }

    private String GetProcessedWord(String word) {
        String processedWord = word;
        if (word.endsWith(";")) {
            processedWord = word.replace(";", " ;");
        } else if (word.endsWith(",")) {
            processedWord = word.replace(",", " ,");
        } else if (word.endsWith("!")) {
            processedWord = word.replace("!", " !");
        } else if (word.endsWith("!!")) {
            processedWord = word.replace("!!", " !");
        } else if (word.endsWith(".")) {
            processedWord = word.replace(".", " .");
        } else if (word.endsWith("?")) {
            processedWord = word.replace("?", " ?");
        } else if (word.endsWith("??")) {
            processedWord = word.replace("??", " ?");
        } else if (word.endsWith("??!!")) {
            processedWord = word.replace("??!!", " ! ?");
        } else if (word.endsWith("?!")) {
            processedWord = word.replace("?!", " ! ?");
        } else if (word.endsWith("....")) {
            processedWord = this.getObjPOSTagging().getWordLemmatize(word);
        } else {
            for (String emoticon : this.listOfEmoticon) {
                if (word.contains(emoticon)) {
                    processedWord = String.valueOf(word.substring(0, word.indexOf(emoticon))) + " " + emoticon;
                    break;
                }
            }
        }
        return processedWord;
    }

    private boolean isEmo(String word) {
        for (String emoticon : this.listOfEmoticon) {
            if (word.equals(emoticon)) {
                return true;
            }
        }
        return false;
    }

    private String RemoveExclamationFromWord(String word) {
        String processedWord = word;
        if (word.contains("!"))
            processedWord = word.replace("!", "");
        return processedWord;
    }

    private boolean IsPersonalPronounNeighbour(String sentence, int forwardSearchRange, int backwardSearchRange, int modifiedWordPosition) {
        String[] wordsInSentence = sentence.trim().split(" ");
        boolean IsPersonalPronounNeighbour = false;
        for (int i = modifiedWordPosition - 1; i >= backwardSearchRange; i--) {
            if (IsPersonalPronoun(wordsInSentence[i].toLowerCase())) {
                IsPersonalPronounNeighbour = true;
                break;
            }
        }
        if (!IsPersonalPronounNeighbour) {
            String word = wordsInSentence[modifiedWordPosition - 1].toLowerCase();
            if (word.indexOf("problem") != -1 || word.indexOf("error") != -1) {
                if (modifyPEWord(wordsInSentence[modifiedWordPosition].toLowerCase())) {
                    IsPersonalPronounNeighbour = true;
                }
            }
        }
        return IsPersonalPronounNeighbour;
    }

    public boolean modifyPEWord(String word) {
        String[] beVerb = {"is", "are", "was", "were"};
        for (int i = 0; i < beVerb.length; i++) {
            if (word.equals(beVerb[i])) {
                return true;
            }
        }
        return false;
    }

    private boolean IsPersonalPronoun(String word) {
        String[] personalPronoun = {"i", "im", "i'm", "my", "you", "he", "she", "we", "they", "mine", "their", "his", "her", "our", "your"};
        for (int i = 0; i < personalPronoun.length; i++) {
            if (word.equals(personalPronoun[i])) {
                return true;
            }
        }
        return false;
    }

    private boolean IsNeutralizerNeighbour(String sentence, int backwardSearchRange, int modifiedWordPosition) {
        String[] wordsInSentence = sentence.trim().split(" ");
        boolean IsNeutralizernNeighbour = false;
        for (int i = modifiedWordPosition - 1; i >= backwardSearchRange; i--) {
            if (wordsInSentence[i].toLowerCase().equals("could") || wordsInSentence[i].toLowerCase().equals("would") || wordsInSentence[i].toLowerCase().equals("might") || wordsInSentence[i].toLowerCase().equals("seem") || wordsInSentence[i].toLowerCase().equals("seems")) {
                IsNeutralizernNeighbour = true;
                break;
            }
        }
        return IsNeutralizernNeighbour;
    }

    private boolean doesWordContainMoreThanOrEqualToTwoUpperCaseLetter(String word) {
        int upperCaseLetterCounter = 0;
        boolean doesWordContainMoreThanTwoUpperCaseLetter = false;
        for (int i = 0; i < word.length(); i++) {
            if (Character.isUpperCase(word.charAt(i))) {
                upperCaseLetterCounter++;
                if (upperCaseLetterCounter >= 2) {
                    doesWordContainMoreThanTwoUpperCaseLetter = true;
                    break;
                }
            }
        }
        return doesWordContainMoreThanTwoUpperCaseLetter;
    }

    public Map GetModifiedTermsMap(String pathToModifiedTermsMap) throws Exception {
        String[] fileText = null;
        Map<String, String> modifiedTermsMap = new HashMap<>();
        try {
            fileText = GetFileText(pathToModifiedTermsMap).split(System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
        }
        int j;
        int i;
        String[] arrayOfString1;
        for (i = (arrayOfString1 = fileText).length, j = 0; j < i; ) {
            String lineofWordMap = arrayOfString1[j];
            modifiedTermsMap.put(lineofWordMap.split("\\t")[0], lineofWordMap.split("\\t")[1]);
            j++;
        }
        return modifiedTermsMap;
    }

    public ArrayList<String> GetEmoTerms(String pathToEmoTerms) throws Exception {
        String[] fileText = null;
        ArrayList<String> emoTerms = new ArrayList<>();
        try {
            fileText = GetFileText(pathToEmoTerms).split(System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
        }
        int j;
        int i;
        String[] arrayOfString1;
        for (i = (arrayOfString1 = fileText).length, j = 0; j < i; j++) {
            String lineofWordMap = arrayOfString1[j];
            emoTerms.add(lineofWordMap.split("\\t")[0]);
        }
        return emoTerms;
    }

    //  private String addBlank(String emo) {
//	  return emo.replace(""," ").trim();
//  }
//  private String toRegular(String text) {
//	  if(text.indexOf("[")!=-1) {
//		  text=text.replaceAll("\\[", "\\\\[");
//	  }
//	  if(text.indexOf("]")!=-1) {
//		  text=text.replaceAll("\\]", "\\\\]");
//	  }
//	  if(text.indexOf("(")!=-1) {
//		  text=text.replaceAll("\\(", "\\\\(");
//	  }
//	  if(text.indexOf(")")!=-1) {
//		  text=text.replaceAll("\\)", "\\\\)");
//	  }
//	  return text;
//  }
    public String GetFileText(String filePath) throws Exception {
        String rawLine = "";
        String currentLine = "";
        BufferedReader objBufferReader = null;
        try {
            objBufferReader = new BufferedReader(new FileReader(filePath));
            while ((currentLine = objBufferReader.readLine()) != null)
                rawLine = String.valueOf(rawLine) + currentLine + System.lineSeparator();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            throw new Exception("File Reading Problem");
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            objBufferReader.close();
        }
        return rawLine;
    }

    public POSTagging getObjPOSTagging() {
        return objPOSTagging;
    }

    public void setObjPOSTagging(POSTagging objPOSTagging) {
        this.objPOSTagging = objPOSTagging;
    }

    private boolean isTechWord(String word) {
        return isStartWthCapLetter(word) || isStartWthSmallLetter(word);
    }

    private boolean isStartWthCapLetter(String word) {
        Pattern StartWthCapLetterp;
        Matcher StartWthCapLetterm;
        StartWthCapLetterp = Pattern.compile(this.regExForCamelCaseStartWithCapLetter);
        StartWthCapLetterm = StartWthCapLetterp.matcher(word);
        if (StartWthCapLetterm.matches()) {
            return true;
        }
        return false;
    }

    private boolean isStartWthSmallLetter(String word) {
        Pattern StartWthSmallLetterp;
        Matcher StartWthSmallLetterm;
        StartWthSmallLetterp = Pattern.compile(this.regExForCamelCaseStartWithSmallLetter);
        StartWthSmallLetterm = StartWthSmallLetterp.matcher(word);
        if (StartWthSmallLetterm.matches()) {
            return true;
        }
        return false;
    }
}