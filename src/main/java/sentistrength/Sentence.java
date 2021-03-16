
package sentistrength;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import preprocess.POSTagging;
import util.Sort;
import util.StringIndex;
import weka.Arff;

public class Sentence {
    private Term[] term;
    private String mySentence = "";
    private boolean beforeisPunc = false;
    public boolean containNeg = false;
    private int hdfscount = 0;
    private int hdfsflag = 0;
    private boolean butflag = false;
    private int negacount = -1;
    private int negspace = 3;
    private POSTagging pt = new POSTagging();
    private boolean[] bgSpaceAfterTerm;
    private int igTermCount = 0;
    private int igPositiveSentiment = 0;
    private int igNegativeSentiment = 0;
    private boolean bgNothingToClassify = true;
    private ClassificationResources resources;
    private ClassificationOptions options;
    private int[] igSentimentIDList;
    private int igSentimentIDListCount = 0;
    private boolean bSentimentIDListMade = false;
    private boolean[] bgIncludeTerm;
    private boolean bgIdiomsApplied = false;
    private boolean bgObjectEvaluationsApplied = false;
    private String sgClassificationRationale = "";
    private boolean bgLydian = false;
    private boolean miss_pos = true;
    private String[] skip = {"adroitbct", "abandonbct", "affabbct", "abominabct", "ecstabct", "abusebct", "overjoyedbct", "devastatbct"};
    private static String[] possSub = {"i", "im", "you", "then", "it", "we", "she", "he", "they"};

    public boolean isBgLydian() {
        return bgLydian;
    }

    public void setBgLydian(boolean bgLydian) {
        this.bgLydian = bgLydian;
    }

    public int getIgTermCount() {
        return igTermCount;
    }

    public String getMySentence() {
        return mySentence;
    }

    public void setMySentence(String mySentence) {
        this.mySentence = mySentence;
    }

    public boolean isButflag() {
        return butflag;
    }

    public void setButflag(boolean butflag) {
        this.butflag = butflag;
    }

    public Sentence() {
    }

    public void addSentenceToIndex(UnusedTermsClassificationIndex unusedTermClassificationIndex) {
        for (int i = 1; i <= this.igTermCount; ++i) {
            unusedTermClassificationIndex.addTermToNewTermIndex(this.term[i].getText());
        }

    }

    public int addToStringIndex(StringIndex stringIndex, TextParsingOptions textParsingOptions, boolean bRecordCount, boolean bArffIndex) {
        String sEncoded = "";
        int iStringPos = 1;
        int iTermsChecked = 0;
        if (textParsingOptions.bgIncludePunctuation && textParsingOptions.igNgramSize == 1 && !textParsingOptions.bgUseTranslations && !textParsingOptions.bgAddEmphasisCode) {
            for (int i = 1; i <= this.igTermCount; ++i) {
                stringIndex.addString(this.term[i].getText(), bRecordCount);
            }

            iTermsChecked = this.igTermCount;
        } else {
            String sText = "";
            int iCurrentTerm = 0;
            int iTermCount = 0;
            while (iCurrentTerm < this.igTermCount) {
                ++iCurrentTerm;
                if (textParsingOptions.bgIncludePunctuation || !this.term[iCurrentTerm].isPunctuation()) {
                    ++iTermCount;
                    if (iTermCount > 1) {
                        sText = sText + " ";
                    } else {
                        sText = "";
                    }

                    if (textParsingOptions.bgUseTranslations) {
                        sText = sText + this.term[iCurrentTerm].getTranslation();
                    } else {
                        sText = sText + this.term[iCurrentTerm].getOriginalText();
                    }

                    if (textParsingOptions.bgAddEmphasisCode && this.term[iCurrentTerm].containsEmphasis()) {
                        sText = sText + "+";
                    }
                }

                if (iTermCount == textParsingOptions.igNgramSize) {
                    if (bArffIndex) {
                        sEncoded = Arff.arffSafeWordEncode(sText.toLowerCase(), false);
                        iStringPos = stringIndex.findString(sEncoded);
                        iTermCount = 0;
                        if (iStringPos > -1) {
                            stringIndex.add1ToCount(iStringPos);
                        }
                    } else {
                        stringIndex.addString(sText.toLowerCase(), bRecordCount);
                        iTermCount = 0;
                    }

                    iCurrentTerm += 1 - textParsingOptions.igNgramSize;
                    ++iTermsChecked;
                }
            }
        }

        return iTermsChecked;
    }

    public void setSentence(String sSentence, ClassificationResources classResources, ClassificationOptions newClassificationOptions) {
        this.resources = classResources;
        this.options = newClassificationOptions;
        if (this.options.bgAlwaysSplitWordsAtApostrophes && sSentence.indexOf("'") >= 0) {
            sSentence = sSentence.replace("'", " ");
        }
        String[] sSegmentList = sSentence.split(" ");
        int iSegmentListLength = sSegmentList.length;
        int iMaxTermListLength = sSentence.length() + 1;
        this.term = new Term[iMaxTermListLength];
        this.bgSpaceAfterTerm = new boolean[iMaxTermListLength];
        int iPos = 0;
        this.igTermCount = 0;

        for (int iSegment = 0; iSegment < iSegmentListLength; ++iSegment) {
            for (iPos = 0; iPos >= 0 && iPos < sSegmentList[iSegment].length(); this.bgSpaceAfterTerm[this.igTermCount] = false) {
                this.term[++this.igTermCount] = new Term();
                int iOffset = this.term[this.igTermCount].extractNextWordOrPunctuationOrEmoticon(sSegmentList[iSegment].substring(iPos), this.resources, this.options);
                if (iOffset < 0) {
                    iPos = iOffset;
                } else {
                    iPos += iOffset;
                }
            }

            this.bgSpaceAfterTerm[this.igTermCount] = true;
        }

        this.bgSpaceAfterTerm[this.igTermCount] = false;
    }

    public int[] getSentimentIDList() {
        if (!this.bSentimentIDListMade) {
            this.makeSentimentIDList();
        }

        return this.igSentimentIDList;
    }

    public void makeSentimentIDList() {
        int iSentimentIDTemp = 0;
        this.igSentimentIDListCount = 0;

        int i;
        for (i = 1; i <= this.igTermCount; ++i) {
            if (this.term[i].getSentimentID() > 0) {
                ++this.igSentimentIDListCount;
            }
        }

        if (this.igSentimentIDListCount > 0) {
            this.igSentimentIDList = new int[this.igSentimentIDListCount + 1];
            this.igSentimentIDListCount = 0;

            for (i = 1; i <= this.igTermCount; ++i) {
                iSentimentIDTemp = this.term[i].getSentimentID();
                if (iSentimentIDTemp > 0) {
                    for (int j = 1; j <= this.igSentimentIDListCount; ++j) {
                        if (iSentimentIDTemp == this.igSentimentIDList[j]) {
                            iSentimentIDTemp = 0;
                            break;
                        }
                    }

                    if (iSentimentIDTemp > 0) {
                        this.igSentimentIDList[++this.igSentimentIDListCount] = iSentimentIDTemp;
                    }
                }
            }

            Sort.quickSortInt(this.igSentimentIDList, 1, this.igSentimentIDListCount);
        }

        this.bSentimentIDListMade = true;
    }

    public String getTaggedSentence() {
        String sTagged = "";

        for (int i = 1; i <= this.igTermCount; ++i) {
            if (this.bgSpaceAfterTerm[i]) {
                sTagged = sTagged + this.term[i].getTag() + " ";
            } else {
                sTagged = sTagged + this.term[i].getTag();
            }
        }

        return sTagged + "<br>";
    }

    public String getClassificationRationale() {
        return this.sgClassificationRationale;
    }

    public String getTranslatedSentence() {
        String sTranslated = "";

        for (int i = 1; i <= this.igTermCount; ++i) {
            if (this.term[i].isWord()) {
                sTranslated = sTranslated + this.term[i].getTranslatedWord();
            } else if (this.term[i].isPunctuation()) {
                sTranslated = sTranslated + this.term[i].getTranslatedPunctuation();
            } else if (this.term[i].isEmoticon()) {
                sTranslated = sTranslated + this.term[i].getEmoticon();
            }

            if (this.bgSpaceAfterTerm[i]) {
                sTranslated = sTranslated + " ";
            }
        }

        return sTranslated + "<br>";
    }

    public void recalculateSentenceSentimentScore() {
        this.calculateSentenceSentimentScore();
    }

    public void reClassifyClassifiedSentenceForSentimentChange(int iSentimentWordID) {
        if (this.igNegativeSentiment == 0) {
            this.calculateSentenceSentimentScore();
        } else {
            if (!this.bSentimentIDListMade) {
                this.makeSentimentIDList();
            }

            if (this.igSentimentIDListCount != 0) {
                if (Sort.i_FindIntPositionInSortedArray(iSentimentWordID, this.igSentimentIDList, 1, this.igSentimentIDListCount) >= 0) {
                    this.calculateSentenceSentimentScore();
                }

            }
        }
    }

    private String[] deleteArrayNull(String string[]) {
        String strArr[] = string;
        ArrayList<String> strList = new ArrayList<String>();
        for (int i = 0; i < strArr.length; i++) {
            strList.add(strArr[i]);
        }
        while (strList.remove(null)) ;
        while (strList.remove("")) ;
        String strArrLast[] = strList.toArray(new String[strList.size()]);
        return strArrLast;
    }

    public int getSentencePositiveSentiment() {
        if (this.igPositiveSentiment == 0) {
            String[] butsens = null;
            if (options.bgDealWithConj) {
                if (mySentence.toLowerCase().indexOf("however") != -1) {
                    mySentence = mySentence.replaceAll("(?i)however", "but ");
                }
                if (mySentence.toLowerCase().indexOf("but ") != -1) {
                    butsens = deleteArrayNull(mySentence.split("(?i)but "));
                }
            }
            if (butsens != null && butsens.length > 1) {
                int butsenslength = butsens.length;
                int[] iPos = new int[butsenslength];
                int[] iNeg = new int[butsenslength];
                int diff = 0;
                int iPosMax = 0;
                int iNegMax = 0;
                Sentence[] sentence = new Sentence[butsenslength];
                for (int i = 0; i < butsenslength; i++) {
                    sentence[i] = new Sentence();
                    sentence[i].setSentence(butsens[i], this.resources, this.options);
                    sentence[i].setMySentence(butsens[i]);
                    iPos[i] = sentence[i].getSentencePositiveSentiment();
                    iNeg[i] = sentence[i].getSentenceNegativeSentiment();
                    if ((iPos[i] != 1 || iNeg[i] != -1) && sentence[i].isBgLydian()) {
                        setBgLydian(true);
                    }
                }
                for (int i = 1; i < butsenslength; i++) {
                    if (iPos[i] > -iNeg[i]) {
                        diff = Math.abs(iPos[i] + iNeg[i]);
                        if (iNeg[i - 1] != -1 && (iPos[i - 1] != 1 || diff >= 2)) {
                            if (diff >= 2) {
                                iNeg[i - 1] += diff;
                            } else {
                                iNeg[i - 1] += 1;
                            }
                        }
                    } else if (iPos[i] < -iNeg[i]) {
                        diff = Math.abs(iPos[i] + iNeg[i]);
                        if (iPos[i - 1] != 1 && (-iNeg[i - 1] != 1 || diff >= 2)) {
                            if (diff >= 2) {
                                iPos[i - 1] -= diff;
                            } else {
                                iPos[i - 1] -= 1;
                            }
                        }
                    }
                }
                for (int i = 0; i < butsenslength; i++) {
                    if (iNegMax > iNeg[i]) {
                        iNegMax = iNeg[i];
                    }
                    if (iPosMax < iPos[i]) {
                        iPosMax = iPos[i];
                    }
                }
                this.igPositiveSentiment = iPosMax;
                this.igNegativeSentiment = iNegMax;
                return this.igPositiveSentiment;
            } else {
                this.calculateSentenceSentimentScore();
            }
        }
        return this.igPositiveSentiment;
    }

    public int getSentenceNegativeSentiment() {
        if (this.igNegativeSentiment == 0) {
            this.calculateSentenceSentimentScore();
        }
        return this.igNegativeSentiment;
    }

    private void markTermsValidToClassify() {
        this.bgIncludeTerm = new boolean[this.igTermCount + 1];
        int iTermsSinceValid;
        if (this.options.bgIgnoreSentencesWithoutKeywords) {
            this.bgNothingToClassify = true;

            int iTerm;
            for (iTermsSinceValid = 1; iTermsSinceValid <= this.igTermCount; ++iTermsSinceValid) {
                this.bgIncludeTerm[iTermsSinceValid] = false;
                if (this.term[iTermsSinceValid].isWord()) {
                    for (iTerm = 0; iTerm < this.options.sgSentimentKeyWords.length; ++iTerm) {
                        if (this.term[iTermsSinceValid].matchesString(this.options.sgSentimentKeyWords[iTerm], true)) {
                            this.bgIncludeTerm[iTermsSinceValid] = true;
                            this.bgNothingToClassify = false;
                        }
                    }
                }
            }

            if (!this.bgNothingToClassify) {
                iTermsSinceValid = 100000;

                for (iTerm = 1; iTerm <= this.igTermCount; ++iTerm) {
                    if (this.bgIncludeTerm[iTerm]) {
                        iTermsSinceValid = 0;
                    } else if (iTermsSinceValid < this.options.igWordsToIncludeAfterKeyword) {
                        this.bgIncludeTerm[iTerm] = true;
                        if (this.term[iTerm].isWord()) {
                            ++iTermsSinceValid;
                        }
                    }
                }

                iTermsSinceValid = 100000;

                for (iTerm = this.igTermCount; iTerm >= 1; --iTerm) {
                    if (this.bgIncludeTerm[iTerm]) {
                        iTermsSinceValid = 0;
                    } else if (iTermsSinceValid < this.options.igWordsToIncludeBeforeKeyword) {
                        this.bgIncludeTerm[iTerm] = true;
                        if (this.term[iTerm].isWord()) {
                            ++iTermsSinceValid;
                        }
                    }
                }
            }
        } else {
            for (iTermsSinceValid = 1; iTermsSinceValid <= this.igTermCount; ++iTermsSinceValid) {
                this.bgIncludeTerm[iTermsSinceValid] = true;
            }

            this.bgNothingToClassify = false;
        }

    }

    private void calculateSentenceSentimentScore() {
        if (this.options.bgExplainClassification && this.sgClassificationRationale.length() > 0) {
            this.sgClassificationRationale = "";
        }
        List<Integer> checkPoint = new ArrayList<Integer>();
        this.igNegativeSentiment = 1;
        this.igPositiveSentiment = 1;
        int iWordTotal = 0;
        int iLastBoosterWordScore = 0;
        int iTemp = 0;
        if (this.igTermCount == 0) {
            this.bgNothingToClassify = true;
            this.igNegativeSentiment = -1;
            this.igPositiveSentiment = 1;
        } else {
            this.markTermsValidToClassify();
            if (this.bgNothingToClassify) {
                this.igNegativeSentiment = -1;
                this.igPositiveSentiment = 1;
            } else {
                boolean bSentencePunctuationBoost = false;
                int iWordsSinceNegative = this.options.igMaxWordsBeforeSentimentToNegate + 2;
                float[] fSentiment = new float[this.igTermCount + 1];
                if (this.options.bgUseIdiomLookupTable) {
                    this.overrideTermStrengthsWithIdiomStrengths(false);
                }

                if (this.options.bgUseObjectEvaluationTable) {
                    this.overrideTermStrengthsWithObjectEvaluationStrengths(false);
                }
                String word = "";
                int ifAnchor = -1;
                ArrayList<Integer> skips = new ArrayList<Integer>();
                boolean ifExclamation = false;
                boolean i_start = false;
                boolean i_pretty = false;
                boolean isCurseWord = false;
                for (int iTerm = 1; iTerm <= this.igTermCount; ++iTerm) {
                    if (this.bgIncludeTerm[iTerm]) {
                        int iTermsChecked;
                        if (!this.term[iTerm].isWord()) {
                            iLastBoosterWordScore = 0;
                            if (this.term[iTerm].isEmoticon()) {
                                negacount = -1;
                                iTermsChecked = this.term[iTerm].getEmoticonSentimentStrength();
                                if (iTermsChecked != 0) {
                                    setBgLydian(true);
                                    ++iWordTotal;
                                    fSentiment[iWordTotal] = (float) iTermsChecked;
                                    if (this.options.bgExplainClassification) {
                                        this.sgClassificationRationale = this.sgClassificationRationale + this.term[iTerm].getEmoticon() + " [" + this.term[iTerm].getEmoticonSentimentStrength() + " emoticon]";
                                    }
                                }
                            } else if (this.term[iTerm].isPunctuation()) {
                                if (!this.term[iTerm].punctuationContains(">")) {
                                    if (!this.term[iTerm].punctuationContains("`") && !this.term[iTerm].punctuationContains("'")) {
                                        beforeisPunc = true;
                                    }
                                    if (this.term[iTerm].punctuationContains(".") || this.term[iTerm].punctuationContains(",")) {
                                        negacount = -1;
                                    }
                                    if (this.options.bgLydian) {
                                        i_start = false;
                                    }
                                    if (options.bgDealWithIf) {
                                        if (ifExclamation && iTerm != this.igTermCount) {
                                            if (this.term[iTerm].isInterrupt()) {
                                                ifAnchor = -1;
                                                ifExclamation = false;
                                            } else if (this.term[iTerm].getOriginalText().equals(",")) {
                                                ifAnchor = -1;
                                            }
                                        }
                                    }
                                }
                                if (this.term[iTerm].getPunctuationEmphasisLength() >= this.options.igMinPunctuationWithExclamationToChangeSentenceSentiment && iWordTotal > 0) {
                                    if (this.term[iTerm].punctuationContains("!")) {
                                        setBgLydian(true);
                                        bSentencePunctuationBoost = true;
                                        if (this.options.bgExplainClassification) {
                                            this.sgClassificationRationale = this.sgClassificationRationale + this.term[iTerm].getOriginalText();
                                        }
                                    }
                                } else if (this.options.bgExplainClassification) {
                                    this.sgClassificationRationale = this.sgClassificationRationale + this.term[iTerm].getOriginalText();
                                }
                            }
                        } else {
                            ++iWordTotal;
                            word = this.term[iTerm].getOriginalText().toLowerCase();
                            isCurseWord = IsCurseWord(word);
                            // deal with Subjunctive Mood
                            if (options.bgDealWithIf) {
                                if (word.equals("if") || word.equals("unless")) {
                                    ifAnchor = iWordTotal;
                                    ifExclamation = true;
                                    if (mySentence.indexOf("?") != -1 && mySentence.indexOf("!") == -1) {
                                        break;
                                    }
                                } else {
                                    if (ifAnchor != -1 && iWordTotal - ifAnchor > 2) {
                                        if (deIfAnchor(word)) {
                                            ifAnchor = -1;
                                        } else if (!this.term[iTerm].getOriginalText().equals(word.toUpperCase()) && !word.equals(this.term[iTerm].getOriginalText())) {
                                            ifExclamation = false;
                                            ifAnchor = -1;
                                        }
                                    }
                                }
                                if (ifAnchor != -1 && !isCurseWord) {
                                    continue;
                                }
                            }

                            if (this.options.bgLydian && !isBgLydian()) {
                                if (word.equals("please") || word.equals("plz")) {
                                    continue;
                                }
                                if (!i_start) i_start = dealWith_I(iTerm);
                                if (beforeisPunc) {
                                    checkPoint.add(iWordTotal);
                                }
                            }
                            boolean varicondi;
                            if (options.bgDealWithCap) {
                                varicondi = beforeisPunc || iTerm <= 2;
                            } else {
                                varicondi = iTerm == 1;
                            }
                            if (varicondi || !this.term[iTerm].isProperNoun() || this.term[iTerm - 1].getOriginalText().equals(":") || this.term[iTerm - 1].getOriginalText().length() > 3 && this.term[iTerm - 1].getOriginalText().substring(0, 1).equals("@")) {
                                boolean excmatch = word.matches("(excuse).*");
                                fSentiment[iWordTotal] = (float) this.term[iTerm].getSentimentValue();
                                if (isHave(word, skip)) {
                                    skips.add(iWordTotal);
                                    continue;
                                }
                                //deal with polysemy
                                if (options.bgDealWithPolysemy) {
                                    if (fSentiment[iWordTotal] != 0) {
                                        if (word.indexOf("miss") != -1) {
                                            fSentiment[iWordTotal] = turnMissBycontext(iTerm, 3);
                                        } else if (word.equals("spite") || word.indexOf("kind") != -1) {
                                            fSentiment[iWordTotal] = turnOfBycontext(iTerm, this.term[iTerm].getSentimentValue());
                                        } else if (word.matches("like")) {
                                            fSentiment[iWordTotal] = pt.turnLikeBycontext(mySentence, word);
                                        } else if (word.equals("lying")) {
                                            fSentiment[iWordTotal] = pt.turnLyingBycontext(mySentence, word);
                                        } else if (word.indexOf("force") != -1 || word.indexOf("block") != -1) {
                                            fSentiment[iWordTotal] = pt.turnVerbBycontext(mySentence, word, this.term[iTerm].getSentimentValue());
                                        } else if (word.equals("pretty") || word.equals("super")) {
                                            fSentiment[iWordTotal] = turnPrettyBycontext(iTerm, mySentence, word);
                                            if (fSentiment[iWordTotal] == 0) this.term[iTerm].setBoosterWordScore(2);
                                        }
                                    }
                                }

                                // deal with negations
                                if (options.bgDealWithNegative) {
                                    boolean isNeg = false;
                                    if (word.equals("no") || word.equals("nothing") || word.equals("without")) {
                                        negacount = negspace - 1;
                                        isNeg = true;
                                    } else if (this.term[iTerm].isNegatingWord()) {
                                        negacount = 0;
                                        isNeg = true;
                                    } else if (word.length() > 3) {
                                        String tail = word.substring(word.length() - 2, word.length());
                                        if (tail.equals("'t")) {
                                            negacount = 0;
                                            isNeg = true;
                                        }
                                    }
                                    if (!isNeg) {
                                        if (negacount == negspace) negacount = -1;
                                        if (negacount >= 0 && negacount < negspace) {
                                            if (!word.equals("to")) {
                                                negacount += 1;
                                            }
                                            if (!isCurseWord && !excmatch) {
                                                fSentiment[iWordTotal] = 0;
                                            }
                                        }
                                    }
                                }

                                if (options.bgDealWithdhsf && isCurseWord) {
                                    if (word.indexOf("fuck") != -1) {
                                        hdfsflag = 2;
                                        fSentiment[iWordTotal] = 0;
                                    } else {
                                        hdfsflag = 1;
                                        fSentiment[iWordTotal] = 0;
                                    }
                                }

                                if (this.options.bgExplainClassification) {
                                    iTemp = this.term[iTerm].getSentimentValue();
                                    if (iTemp < 0) {
                                        --iTemp;
                                    } else {
                                        ++iTemp;
                                    }
                                    if (iTemp == 1) {
                                        this.sgClassificationRationale = this.sgClassificationRationale + this.term[iTerm].getOriginalText() + " ";
                                    } else {
                                        this.sgClassificationRationale = this.sgClassificationRationale + this.term[iTerm].getOriginalText() + "[" + iTemp + "] ";
                                    }
                                }
                                beforeisPunc = false;
                            } else if (this.options.bgExplainClassification) {
                                this.sgClassificationRationale = this.sgClassificationRationale + this.term[iTerm].getOriginalText() + " [proper noun] ";
                            }

                            if (this.options.bgMultipleLettersBoostSentiment && this.term[iTerm].getWordEmphasisLength() >= this.options.igMinRepeatedLettersForBoost && (iTerm == 1 || !this.term[iTerm - 1].isPunctuation() || !this.term[iTerm - 1].getOriginalText().equals("@"))) {
                                String sEmphasis = this.term[iTerm].getWordEmphasis().toLowerCase();
                                if (sEmphasis.indexOf("xx") < 0 && sEmphasis.indexOf("ww") < 0 && sEmphasis.indexOf("ha") < 0) {
                                    if (fSentiment[iWordTotal] < 0.0F) {
                                        fSentiment[iWordTotal] = (float) ((double) fSentiment[iWordTotal] - 0.6D);
                                        if (this.options.bgExplainClassification) {
                                            this.sgClassificationRationale = this.sgClassificationRationale + "[-0.6 spelling emphasis] ";
                                        }
                                    } else if (fSentiment[iWordTotal] > 0.0F) {
                                        fSentiment[iWordTotal] = (float) ((double) fSentiment[iWordTotal] + 0.6D);
                                        if (this.options.bgExplainClassification) {
                                            this.sgClassificationRationale = this.sgClassificationRationale + "[+0.6 spelling emphasis] ";
                                        }
                                    } else if (this.options.igMoodToInterpretNeutralEmphasis > 0) {
                                        fSentiment[iWordTotal] = (float) ((double) fSentiment[iWordTotal] + 0.6D);
                                        if (this.options.bgExplainClassification) {
                                            this.sgClassificationRationale = this.sgClassificationRationale + "[+0.6 spelling mood emphasis] ";
                                        }
                                    } else if (this.options.igMoodToInterpretNeutralEmphasis < 0) {
                                        fSentiment[iWordTotal] = (float) ((double) fSentiment[iWordTotal] - 0.6D);
                                        if (this.options.bgExplainClassification) {
                                            this.sgClassificationRationale = this.sgClassificationRationale + "[-0.6 spelling mood emphasis] ";
                                        }
                                    }
                                }
                            }
                            int var10002;
                            if (this.options.bgCapitalsBoostTermSentiment && fSentiment[iWordTotal] != 0.0F && this.term[iTerm].isAllCapitals()) {
                                if (fSentiment[iWordTotal] > 0.0F) {
                                    var10002 = (int) fSentiment[iWordTotal]++;
                                    if (this.options.bgExplainClassification) {
                                        this.sgClassificationRationale = this.sgClassificationRationale + "[+1 CAPITALS] ";
                                    }
                                } else {
                                    var10002 = (int) fSentiment[iWordTotal]--;
                                    if (this.options.bgExplainClassification) {
                                        this.sgClassificationRationale = this.sgClassificationRationale + "[-1 CAPITALS] ";
                                    }
                                }
                            }
                            if (this.options.bgBoosterWordsChangeEmotion) {
                                if (iLastBoosterWordScore != 0) {
                                    if (fSentiment[iWordTotal] > 0.0F) {
                                        fSentiment[iWordTotal] += (float) iLastBoosterWordScore;
                                        checkPoint.add(iWordTotal);
                                        if (this.options.bgExplainClassification) {
                                            this.sgClassificationRationale = this.sgClassificationRationale + "[+" + iLastBoosterWordScore + " booster word] ";
                                        }
                                    } else if (fSentiment[iWordTotal] < 0.0F) {
                                        fSentiment[iWordTotal] -= (float) iLastBoosterWordScore;
                                        checkPoint.add(iWordTotal);
                                        if (this.options.bgExplainClassification) {
                                            this.sgClassificationRationale = this.sgClassificationRationale + "[-" + iLastBoosterWordScore + " booster word] ";
                                        }
                                    }
                                }
                                iLastBoosterWordScore = this.term[iTerm].getBoosterWordScore();
                            }
                            if (this.options.bgNegatingWordsOccurBeforeSentiment) {
                                if (this.options.bgNegatingWordsFlipEmotion) {
                                    if (iWordsSinceNegative <= this.options.igMaxWordsBeforeSentimentToNegate) {
                                        fSentiment[iWordTotal] = -fSentiment[iWordTotal] * this.options.fgStrengthMultiplierForNegatedWords;
                                        if (this.options.bgExplainClassification) {
                                            this.sgClassificationRationale = this.sgClassificationRationale + "[*-" + this.options.fgStrengthMultiplierForNegatedWords + " approx. negated multiplier] ";
                                        }
                                    }
                                } else {
                                    if (this.options.bgNegatingNegativeNeutralisesEmotion && fSentiment[iWordTotal] < 0.0F && iWordsSinceNegative <= this.options.igMaxWordsBeforeSentimentToNegate) {
                                        fSentiment[iWordTotal] = 0.0F;
                                        if (this.options.bgExplainClassification) {
                                            this.sgClassificationRationale = this.sgClassificationRationale + "[=0 negation] ";
                                        }
                                    }
                                    if (this.options.bgNegatingPositiveFlipsEmotion && fSentiment[iWordTotal] > 0.0F && iWordsSinceNegative <= this.options.igMaxWordsBeforeSentimentToNegate) {
                                        fSentiment[iWordTotal] = -fSentiment[iWordTotal] * this.options.fgStrengthMultiplierForNegatedWords;
                                        if (this.options.bgExplainClassification) {
                                            this.sgClassificationRationale = this.sgClassificationRationale + "[*-" + this.options.fgStrengthMultiplierForNegatedWords + " approx. negated multiplier] ";
                                        }
                                    }
                                }
                            }
                            if (this.term[iTerm].isNegatingWord()) {
                                iWordsSinceNegative = -1;
                            }
                            if (iLastBoosterWordScore == 0) {
                                ++iWordsSinceNegative;
                            }
                            if (this.term[iTerm].isNegatingWord() && this.options.bgNegatingWordsOccurAfterSentiment) {
                                iTermsChecked = 0;
                                for (int iPriorWord = iWordTotal - 1; iPriorWord > 0; --iPriorWord) {
                                    if (this.options.bgNegatingWordsFlipEmotion) {
                                        fSentiment[iPriorWord] = -fSentiment[iPriorWord] * this.options.fgStrengthMultiplierForNegatedWords;
                                        if (this.options.bgExplainClassification) {
                                            this.sgClassificationRationale = this.sgClassificationRationale + "[*-" + this.options.fgStrengthMultiplierForNegatedWords + " approx. negated multiplier] ";
                                        }
                                    } else {
                                        if (this.options.bgNegatingNegativeNeutralisesEmotion && fSentiment[iPriorWord] < 0.0F) {
                                            fSentiment[iPriorWord] = 0.0F;
                                            if (this.options.bgExplainClassification) {
                                                this.sgClassificationRationale = this.sgClassificationRationale + "[=0 negation] ";
                                            }
                                        }

                                        if (this.options.bgNegatingPositiveFlipsEmotion && fSentiment[iPriorWord] > 0.0F) {
                                            fSentiment[iPriorWord] = -fSentiment[iPriorWord] * this.options.fgStrengthMultiplierForNegatedWords;
                                            if (this.options.bgExplainClassification) {
                                                this.sgClassificationRationale = this.sgClassificationRationale + "[*-" + this.options.fgStrengthMultiplierForNegatedWords + " approx. negated multiplier] ";
                                            }
                                        }
                                    }

                                    ++iTermsChecked;
                                    if (iTermsChecked > this.options.igMaxWordsAfterSentimentToNegate) {
                                        break;
                                    }
                                }
                            }
                            if (this.options.bgAllowMultipleNegativeWordsToIncreaseNegativeEmotion && fSentiment[iWordTotal] < -1.0F && iWordTotal > 1 && fSentiment[iWordTotal - 1] < -1.0F) {
                                var10002 = (int) fSentiment[iWordTotal]--;
                                if (this.options.bgExplainClassification) {
                                    this.sgClassificationRationale = this.sgClassificationRationale + "[-1 consecutive negative words] ";
                                }
                            }

                            if (this.options.bgAllowMultiplePositiveWordsToIncreasePositiveEmotion && fSentiment[iWordTotal] > 1.0F && iWordTotal > 1 && fSentiment[iWordTotal - 1] > 1.0F) {
                                var10002 = (int) fSentiment[iWordTotal]++;
                                if (this.options.bgExplainClassification) {
                                    this.sgClassificationRationale = this.sgClassificationRationale + "[+1 consecutive positive words] ";
                                }
                            }
                            if (this.options.bgLydian && !isBgLydian()) {
                                if (word.equals("because") || word.equals("but") || word.equals("so")) {
                                    beforeisPunc = true;
                                }
                                if (IsCurseWord(word)) setBgLydian(true);
                                if (fSentiment[iWordTotal] != 0.0F) {
                                    if (pt.isLadian(mySentence, word)) {
                                        checkPoint.add(iWordTotal);
                                    }
                                    if (i_start) {
                                        checkPoint.add(iWordTotal);
                                    }
                                }
                            }

                        }
                    }
                }
                if (bSentencePunctuationBoost) {
                    int finalsentipos = -1;
                    for (int i = 1; i <= iWordTotal; ++i) {
                        if (fSentiment[i] != 0.0F) {
                            finalsentipos = i;
                        }
                    }
                    if (finalsentipos != -1) {
                        if (fSentiment[finalsentipos] > 0.0F) {
                            fSentiment[finalsentipos]++;
                        } else {
                            fSentiment[finalsentipos]--;
                        }
                    }
                }

                float fTotalNeg = 0.0F;
                float fTotalPos = 0.0F;
                float fMaxNeg = 0.0F;
                float fMaxPos = 0.0F;
                float sentiDentity = 0.0F;
                int sentiWords = 0;
                int iPosWords = 0;
                int iNegWords = 0;
                for (int i = 1; i <= iWordTotal; ++i) {
                    if (fSentiment[i] != 0.0F && !skips.contains(i)) {
                        sentiWords++;
                    }
                    if (fSentiment[i] < 0.0F) {
                        fTotalNeg += fSentiment[i];
                        ++iNegWords;
                        if (fMaxNeg > fSentiment[i]) {
                            fMaxNeg = fSentiment[i];
                        }
                    } else if (fSentiment[i] > 0.0F) {
                        fTotalPos += fSentiment[i];
                        ++iPosWords;
                        if (fMaxPos < fSentiment[i]) {
                            fMaxPos = fSentiment[i];
                        }
                    }
                }
                if (this.options.bgLydian && !isBgLydian()) {
                    sentiDentity = (float) (sentiWords) / iWordTotal;
                    if (sentiDentity - 0.3 > 0) setBgLydian(true);
                    if (fSentiment[1] != 0.0F && !skips.contains(1)) setBgLydian(true);
                    for (int i = 0; i < checkPoint.size(); i++) {
                        if (fSentiment[checkPoint.get(i)] != 0.0F && !skips.contains(checkPoint.get(i))) {
                            setBgLydian(true);
                            break;
                        }
                    }
                }

                --fMaxNeg;
                ++fMaxPos;
                int var10000 = this.options.igEmotionSentenceCombineMethod;
                this.options.getClass();
                if (var10000 == 1) {
                    if (iPosWords == 0) {
                        this.igPositiveSentiment = 1;
                    } else {
                        this.igPositiveSentiment = (int) Math.round(((double) (fTotalPos + (float) iPosWords) + 0.45D) / (double) iPosWords);
                    }

                    if (iNegWords == 0) {
                        this.igNegativeSentiment = -1;
                    } else {
                        this.igNegativeSentiment = (int) Math.round(((double) (fTotalNeg - (float) iNegWords) + 0.55D) / (double) iNegWords);
                    }
                } else {
                    var10000 = this.options.igEmotionSentenceCombineMethod;
                    this.options.getClass();
                    if (var10000 == 2) {
                        this.igPositiveSentiment = Math.round(fTotalPos) + iPosWords;
                        this.igNegativeSentiment = Math.round(fTotalNeg) - iNegWords;
                    } else {
                        this.igPositiveSentiment = Math.round(fMaxPos);
                        this.igNegativeSentiment = Math.round(fMaxNeg);
                    }
                }
                if (this.options.bgReduceNegativeEmotionInQuestionSentences && this.igNegativeSentiment < -1) {
                    for (int i = 1; i <= this.igTermCount; ++i) {
                        if (this.term[i].isWord()) {
                            if (this.resources.questionWords.questionWord(this.term[i].getTranslatedWord().toLowerCase())) {
                                ++this.igNegativeSentiment;
                                if (this.options.bgExplainClassification) {
                                    this.sgClassificationRationale = this.sgClassificationRationale + "[+1 negative for question word]";
                                }
                                break;
                            }
                        } else if (this.term[i].isPunctuation() && this.term[i].punctuationContains("?")) {
                            ++this.igNegativeSentiment;
                            if (this.options.bgExplainClassification) {
                                this.sgClassificationRationale = this.sgClassificationRationale + "[+1 negative for question mark ?]";
                            }
                            break;
                        }
                    }
                }
                if (miss_pos && this.igPositiveSentiment == 1 && this.options.bgMissCountsAsPlus2) {
                    for (int i = 1; i <= this.igTermCount; ++i) {
                        if (this.term[i].isWord() && this.term[i].getTranslatedWord().toLowerCase().compareTo("miss") == 0) {
                            this.igPositiveSentiment = 2;
                            if (this.options.bgExplainClassification) {
                                this.sgClassificationRationale = this.sgClassificationRationale + "[pos = 2 for term 'miss']";
                            }
                            break;
                        }
                    }
                }
                //存在负面词2
                if (options.bgDealWithdhsf) {
                    if (this.igPositiveSentiment == 1) {
                        if (isCurseWord) {
                            this.igNegativeSentiment = Math.min(this.igNegativeSentiment, -(hdfsflag + 1));
                        }
                    } else {
                        if ((hdfscount == 2) || (hdfscount == 1 && this.igNegativeSentiment < -1)) {
                            this.igNegativeSentiment = Math.min(this.igNegativeSentiment, -(hdfsflag + 1));
                        }
                    }
                }

                if (bSentencePunctuationBoost) {
                    if (this.igPositiveSentiment == -this.igNegativeSentiment && (!options.bgDealWithIf || !ifExclamation)) {
                        if (this.options.igMoodToInterpretNeutralEmphasis > 0) {
                            if (-fTotalNeg > fTotalPos && -fMaxNeg > fMaxPos) {
                                --this.igNegativeSentiment;
                            } else {
                                ++this.igPositiveSentiment;
                            }
                            if (this.options.bgExplainClassification) {
                                this.sgClassificationRationale = this.sgClassificationRationale + "[+1 punctuation mood emphasis] ";
                            }
                        } else if (this.options.igMoodToInterpretNeutralEmphasis < 0) {
                            if (-fTotalNeg < fTotalPos && -fMaxNeg < fMaxPos) {
                                ++this.igPositiveSentiment;
                            } else {
                                --this.igNegativeSentiment;
                            }
                            if (this.options.bgExplainClassification) {
                                this.sgClassificationRationale = this.sgClassificationRationale + "[-1 punctuation mood emphasis] ";
                            }
                        }
                    }
                }
                if (this.igPositiveSentiment == 1 && this.igNegativeSentiment == -1 && this.options.bgExclamationInNeutralSentenceCountsAsPlus2) {
                    for (int i = 1; i <= this.igTermCount; ++i) {
                        if (this.term[i].isPunctuation() && this.term[i].punctuationContains("!")) {
                            this.igPositiveSentiment = 2;
                            if (this.options.bgExplainClassification) {
                                this.sgClassificationRationale = this.sgClassificationRationale + "[pos = 2 for !]";
                            }
                            break;
                        }
                    }
                }

                if (this.igPositiveSentiment == 1 && this.igNegativeSentiment == -1 && this.options.bgYouOrYourIsPlus2UnlessSentenceNegative) {
                    for (int i = 1; i <= this.igTermCount; ++i) {
                        if (this.term[i].isWord()) {
                            String sTranslatedWord = this.term[i].getTranslatedWord().toLowerCase();
                            if (sTranslatedWord.compareTo("you") == 0 || sTranslatedWord.compareTo("your") == 0 || sTranslatedWord.compareTo("whats") == 0) {
                                this.igPositiveSentiment = 2;
                                if (this.options.bgExplainClassification) {
                                    this.sgClassificationRationale = this.sgClassificationRationale + "[pos = 2 for you/your/whats]";
                                }
                                break;
                            }
                        }
                    }
                }
                this.adjustSentimentForIrony();
                var10000 = this.options.igEmotionSentenceCombineMethod;
                this.options.getClass();
                if (var10000 != 2) {
                    if (this.igPositiveSentiment > 5) {
                        this.igPositiveSentiment = 5;
                    }

                    if (this.igNegativeSentiment < -5) {
                        this.igNegativeSentiment = -5;
                    }
                }

                if (this.options.bgExplainClassification) {
                    this.sgClassificationRationale = this.sgClassificationRationale + "[sentence: " + this.igPositiveSentiment + "," + this.igNegativeSentiment + "]";
                }
            }
        }

    }

    private void adjustSentimentForIrony() {
        int iTerm;
        if (this.igPositiveSentiment >= this.options.igMinSentencePosForQuotesIrony) {
            for (iTerm = 1; iTerm <= this.igTermCount; ++iTerm) {
                if (this.term[iTerm].isPunctuation() && this.term[iTerm].getText().indexOf(34) >= 0) {
                    if (this.igNegativeSentiment > -this.igPositiveSentiment) {
                        this.igNegativeSentiment = 1 - this.igPositiveSentiment;
                    }

                    this.igPositiveSentiment = 1;
                    this.sgClassificationRationale = this.sgClassificationRationale + "[Irony change: pos = 1, neg = " + this.igNegativeSentiment + "]";
                    return;
                }
            }
        }

        if (this.igPositiveSentiment >= this.options.igMinSentencePosForPunctuationIrony) {
            for (iTerm = 1; iTerm <= this.igTermCount; ++iTerm) {
                if (this.term[iTerm].isPunctuation() && this.term[iTerm].punctuationContains("!") && this.term[iTerm].getPunctuationEmphasisLength() > 0) {
                    if (this.igNegativeSentiment > -this.igPositiveSentiment) {
                        this.igNegativeSentiment = 1 - this.igPositiveSentiment;
                    }

                    this.igPositiveSentiment = 1;
                    this.sgClassificationRationale = this.sgClassificationRationale + "[Irony change: pos = 1, neg = " + this.igNegativeSentiment + "]";
                    return;
                }
            }
        }

        if (this.igPositiveSentiment >= this.options.igMinSentencePosForTermsIrony) {
            for (iTerm = 1; iTerm <= this.igTermCount; ++iTerm) {
                if (this.resources.ironyList.termIsIronic(this.term[iTerm].getText())) {
                    if (this.igNegativeSentiment > -this.igPositiveSentiment) {
                        this.igNegativeSentiment = 1 - this.igPositiveSentiment;
                    }

                    this.igPositiveSentiment = 1;
                    this.sgClassificationRationale = this.sgClassificationRationale + "[Irony change: pos = 1, neg = " + this.igNegativeSentiment + "]";
                    return;
                }
            }
        }

    }

    public void overrideTermStrengthsWithObjectEvaluationStrengths(boolean recalculateIfAlreadyDone) {
        boolean bMatchingObject = false;
        boolean bMatchingEvaluation = false;
        if (!this.bgObjectEvaluationsApplied || recalculateIfAlreadyDone) {
            for (int iObject = 1; iObject < this.resources.evaluativeTerms.igObjectEvaluationCount; ++iObject) {
                bMatchingObject = false;
                bMatchingEvaluation = false;

                int iTerm;
                for (iTerm = 1; iTerm <= this.igTermCount; ++iTerm) {
                    if (this.term[iTerm].isWord() && this.term[iTerm].matchesStringWithWildcard(this.resources.evaluativeTerms.sgObject[iObject], true)) {
                        bMatchingObject = true;
                        break;
                    }
                }

                if (bMatchingObject) {
                    for (iTerm = 1; iTerm <= this.igTermCount; ++iTerm) {
                        if (this.term[iTerm].isWord() && this.term[iTerm].matchesStringWithWildcard(this.resources.evaluativeTerms.sgObjectEvaluation[iObject], true)) {
                            bMatchingEvaluation = true;
                            break;
                        }
                    }
                }

                if (bMatchingEvaluation) {
                    if (this.options.bgExplainClassification) {
                        this.sgClassificationRationale = this.sgClassificationRationale + "[term weight changed by object/evaluation]";
                    }

                    this.term[iTerm].setSentimentOverrideValue(this.resources.evaluativeTerms.igObjectEvaluationStrength[iObject]);
                }
            }

            this.bgObjectEvaluationsApplied = true;
        }

    }

    public void overrideTermStrengthsWithIdiomStrengths(boolean recalculateIfAlreadyDone) {
        if (!this.bgIdiomsApplied || recalculateIfAlreadyDone) {
            for (int iTerm = 1; iTerm <= this.igTermCount; ++iTerm) {
                if (this.term[iTerm].isWord()) {
                    for (int iIdiom = 1; iIdiom <= this.resources.idiomList.igIdiomCount; ++iIdiom) {
                        if (iTerm + this.resources.idiomList.igIdiomWordCount[iIdiom] - 1 <= this.igTermCount) {
                            boolean bMatchingIdiom = true;

                            int iIdiomTerm;
                            for (iIdiomTerm = 0; iIdiomTerm < this.resources.idiomList.igIdiomWordCount[iIdiom]; ++iIdiomTerm) {
                                if (!this.term[iTerm + iIdiomTerm].matchesStringWithWildcard(this.resources.idiomList.sgIdiomWords[iIdiom][iIdiomTerm], true)) {
                                    bMatchingIdiom = false;
                                    break;
                                }
                            }

                            if (bMatchingIdiom) {
                                if (this.options.bgExplainClassification) {
                                    this.sgClassificationRationale = this.sgClassificationRationale + "[term weight(s) changed by idiom " + this.resources.idiomList.getIdiom(iIdiom) + "]";
                                }

                                this.term[iTerm].setSentimentOverrideValue(this.resources.idiomList.igIdiomStrength[iIdiom]);

                                for (iIdiomTerm = 1; iIdiomTerm < this.resources.idiomList.igIdiomWordCount[iIdiom]; ++iIdiomTerm) {
                                    this.term[iTerm + iIdiomTerm].setSentimentOverrideValue(0);
                                }
                            }
                        }
                    }
                }
            }

            this.bgIdiomsApplied = true;
        }

    }

    public int turnMissBycontext(int iTerm, int scope) {
        if (iTerm + 1 <= this.igTermCount) {
            if (this.term[iTerm + 1].isPunctuation()) {
                return 0;
            } else {
                String word = this.term[iTerm + 1].getOriginalText().toLowerCase();
                String tag = pt.readWordPos(mySentence, word);
                tag = tag.substring(0, 2);
                if (tag.equals("NN") || tag.equals("DT")) {
                    miss_pos = false;
                    return -1;
                } else if (tag.equals("PRP")) {
                    return -1;
                } else {
                    return 0;
                }
            }
        } else {
            return 0;
        }
    }

    public float turnBadBycontext(int iTerm) {
        if (turnOfBycontext(iTerm, -1) == 0) return 0;
        for (int i = 1; i <= 3; i++) {
            if (iTerm >= i && this.term[iTerm - i] != null && this.term[iTerm - i].getText().matches("(?i)want")) {
                return 0;
            }
        }
        return -1;
    }

    public float turnOfBycontext(int iTerm, int ori) {
        if (iTerm + 1 < this.term.length && this.term[iTerm + 1] != null && this.term[iTerm + 1].getText().equals("of")) {
            return 0;
        }
        return ori;
    }

    public int turnPrettyBycontext(int iTerm, String str, String word) {
        if (iTerm + 1 < this.term.length && this.term[iTerm + 1] != null && this.term[iTerm + 1].getSentimentValue() != 0.0F) {
            return 0;
        } else {
            return pt.turnPrettyBycontext(str, word);
        }
    }

    private boolean dealWith_I(int iTerm) {
        if (!isBgLydian()) {
            String word = this.term[iTerm].getText().toLowerCase();
            String next = "";
            int boost = 0;
            float senti = 0.0F;
            if (iTerm < this.igTermCount) {
                next = this.term[iTerm + 1].getText().toLowerCase();
                boost = this.term[iTerm + 1].getBoosterWordScore();
            }
            boolean isIx = word.length() > 2 && word.substring(0, 2).equals("i'");
            if (word.equals("i") || word.equals("im") || isIx) {
                return true;
            } else if (word.equals("always") || word.equals("even") || word.equals("still")) {
                return true;
            }
        }
        return false;
    }

    private boolean deIfAnchor(String word) {
        if (word.length() > 2 && word.substring(0, 2).equals("i'")) {
            return true;
        }
        if (word.length() >= 4 && word.substring(0, 4).equals("that")) {
            return true;
        }
        if (word.length() >= 5 && word.substring(0, 5).equals("there")) {
            return true;
        }
        return isHave(word, possSub);
    }

    public boolean isHave(String word, String[] possWrd) {
        for (int i = 0; i < possWrd.length; i++) {
            if (word.equals(possWrd[i])) {
                return true;
            }
        }
        return false;
    }

    public boolean isContain(String word, String[] possWrd) {
        for (int i = 0; i < possWrd.length; i++) {
            if (word.indexOf(possWrd[i]) != -1) {
                return true;
            }
        }
        return false;
    }

    private boolean IsCurseWord(String word) {
        String[] curseWord = {"fuck", "damn", "shit", "hell"};
        return isContain(word, curseWord);
    }

    private boolean IsEndWith_t(String word) {
        if (word.length() > 3) {
            String tail = word.substring(word.length() - 2, word.length());
            if (tail.equals("'t")) {
                return true;
            }
        }
        return false;
    }
}