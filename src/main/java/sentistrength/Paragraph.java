package sentistrength;

import java.util.ArrayList;
import java.util.Random;

import preprocess.PreProcess;
import util.FileOps;
import util.Sort;
import util.StringIndex;

public class Paragraph {
    static int num = 0;
    static PreProcess ppr = new PreProcess();
    private Sentence[] sentence;
    private int igSentenceCount = 0;
    private int[] igSentimentIDList;
    private int igSentimentIDListCount = 0;
    private boolean bSentimentIDListMade = false;
    private int igPositiveSentiment = 0;
    private int igNegativeSentiment = 0;
    private int igTrinarySentiment = 0;
    private int igScaleSentiment = 0;
    private ClassificationResources resources;
    private ClassificationOptions options;
    private Random generator = new Random();
    private String sgClassificationRationale = "";

    public void addParagraphToIndexWithPosNegValues(UnusedTermsClassificationIndex unusedTermsClassificationIndex, int iCorrectPosClass, int iEstPosClass, int iCorrectNegClass, int iEstNegClass) {
        for (int i = 1; i <= this.igSentenceCount; ++i) {
            this.sentence[i].addSentenceToIndex(unusedTermsClassificationIndex);
        }

        unusedTermsClassificationIndex.addNewIndexToMainIndexWithPosNegValues(iCorrectPosClass, iEstPosClass, iCorrectNegClass, iEstNegClass);
    }

    public void addParagraphToIndexWithScaleValues(UnusedTermsClassificationIndex unusedTermsClassificationIndex, int iCorrectScaleClass, int iEstScaleClass) {
        for (int i = 1; i <= this.igSentenceCount; ++i) {
            this.sentence[i].addSentenceToIndex(unusedTermsClassificationIndex);
        }

        unusedTermsClassificationIndex.addNewIndexToMainIndexWithScaleValues(iCorrectScaleClass, iEstScaleClass);
    }

    public void addParagraphToIndexWithBinaryValues(UnusedTermsClassificationIndex unusedTermsClassificationIndex, int iCorrectBinaryClass, int iEstBinaryClass) {
        for (int i = 1; i <= this.igSentenceCount; ++i) {
            this.sentence[i].addSentenceToIndex(unusedTermsClassificationIndex);
        }

        unusedTermsClassificationIndex.addNewIndexToMainIndexWithBinaryValues(iCorrectBinaryClass, iEstBinaryClass);
    }

    public int addToStringIndex(StringIndex stringIndex, TextParsingOptions textParsingOptions, boolean bRecordCount, boolean bArffIndex) {
        int iTermsChecked = 0;

        for (int i = 1; i <= this.igSentenceCount; ++i) {
            iTermsChecked += this.sentence[i].addToStringIndex(stringIndex, textParsingOptions, bRecordCount, bArffIndex);
        }

        return iTermsChecked;
    }

    public void addParagraphToIndexWithTrinaryValues(UnusedTermsClassificationIndex unusedTermsClassificationIndex, int iCorrectTrinaryClass, int iEstTrinaryClass) {
        for (int i = 1; i <= this.igSentenceCount; ++i) {
            this.sentence[i].addSentenceToIndex(unusedTermsClassificationIndex);
        }

        unusedTermsClassificationIndex.addNewIndexToMainIndexWithTrinaryValues(iCorrectTrinaryClass, iEstTrinaryClass);
    }

    public void setParagraph(String sParagraph, ClassificationResources classResources, ClassificationOptions newClassificationOptions) {
        this.resources = classResources;
        this.options = newClassificationOptions;
        ArrayList<String> processedArray = null;
        ArrayList<String> bctArr = null;
        Sentence[] bctSentence = null;
        try {
            ppr.setBgPreprocess(options.bgPreprocess);
            ppr.init();
            processedArray = ppr.getProcessedArray(sParagraph);
        } catch (Exception e) {
            e.printStackTrace();
        }
        int bctArrSize = ppr.getBctArrSize();
        int[] bctPos = null;
        int[] bctNeg = null;
        boolean[] bctLydian = null;
        if (bctArrSize != 0) {
            bctArr = ppr.getBctSentenceArr();
            bctSentence = new Sentence[bctArrSize];
            bctPos = new int[bctArrSize];
            bctNeg = new int[bctArrSize];
            bctLydian = new boolean[bctArrSize];
            for (int i = 0; i < bctArrSize; i++) {
                bctSentence[i] = new Sentence();
                bctSentence[i].setSentence(bctArr.get(i), this.resources, this.options);
                bctSentence[i].setMySentence(bctArr.get(i));
                int posScore = bctSentence[i].getSentencePositiveSentiment();
                int negScore = bctSentence[i].getSentenceNegativeSentiment();
                bctPos[i] = posScore;
                bctNeg[i] = negScore;
                bctLydian[i] = bctSentence[i].isBgLydian();
                String bctreplace = "";
                if (posScore != 1 || negScore != -1) {
                    bctreplace = scoreConvert(posScore) + " " + scoreConvert(negScore);
                }
                bctArr.set(i, bctreplace);
            }
        }
        int pedArrayLength = processedArray.size();
        this.sentence = new Sentence[pedArrayLength + 1];
        this.igSentenceCount = 0;
        for (int i = 0; i < pedArrayLength; i++) {
            String sNextSentence = processedArray.get(i);
            boolean lydian = false;
            if (bctArrSize != 0) {
                int indexPos = sNextSentence.indexOf("SUBSENTENCE_INDEXOF-");
                while (indexPos >= 0) {
                    int index = Integer.parseInt(sNextSentence.substring(indexPos + 20, indexPos + 21));
                    sNextSentence = sNextSentence.substring(0, indexPos) + bctArr.get(index) + sNextSentence.substring(indexPos + 21, sNextSentence.length());
                    if ((bctPos[index] != 1 || bctNeg[index] != -1) && !lydian && bctLydian[index]) {
                        lydian = true;
                    }
                    indexPos = sNextSentence.indexOf("SUBSENTENCE_INDEXOF-");
                }
            }
            ++this.igSentenceCount;
            this.sentence[this.igSentenceCount] = new Sentence();
            this.sentence[this.igSentenceCount].setSentence(sNextSentence, this.resources, this.options);
            this.sentence[this.igSentenceCount].setMySentence(sNextSentence);
            if (lydian) this.sentence[this.igSentenceCount].setBgLydian(true);
        }
    }

    private String scoreConvert(int score) {
        switch (score) {
            case 1:
                return "";
            case -1:
                return "";
            case 2:
                return "adroitbct";
            case -2:
                return "abandonbct";
            case 3:
                return "affabbct";
            case -3:
                return "abominabct";
            case 4:
                return "ecstabct";
            case -4:
                return "abusebct";
            case 5:
                return "overjoyedbct";
            case -5:
                return "devastatbct";
        }
        return "";
    }

    public int[] getSentimentIDList() {
        if (!this.bSentimentIDListMade) {
            this.makeSentimentIDList();
        }

        return this.igSentimentIDList;
    }

    public String getClassificationRationale() {
        return this.sgClassificationRationale;
    }

    public void makeSentimentIDList() {
        boolean bIsDuplicate = false;
        this.igSentimentIDListCount = 0;

        int i;
        for (i = 1; i <= this.igSentenceCount; ++i) {
            if (this.sentence[i].getSentimentIDList() != null) {
                this.igSentimentIDListCount += this.sentence[i].getSentimentIDList().length;
            }
        }

        if (this.igSentimentIDListCount > 0) {
            this.igSentimentIDList = new int[this.igSentimentIDListCount + 1];
            this.igSentimentIDListCount = 0;

            for (i = 1; i <= this.igSentenceCount; ++i) {
                int[] sentenceIDList = this.sentence[i].getSentimentIDList();
                if (sentenceIDList != null) {
                    for (int j = 1; j < sentenceIDList.length; ++j) {
                        if (sentenceIDList[j] != 0) {
                            bIsDuplicate = false;

                            for (int k = 1; k <= this.igSentimentIDListCount; ++k) {
                                if (sentenceIDList[j] == this.igSentimentIDList[k]) {
                                    bIsDuplicate = true;
                                    break;
                                }
                            }

                            if (!bIsDuplicate) {
                                this.igSentimentIDList[++this.igSentimentIDListCount] = sentenceIDList[j];
                            }
                        }
                    }
                }
            }

            Sort.quickSortInt(this.igSentimentIDList, 1, this.igSentimentIDListCount);
        }

        this.bSentimentIDListMade = true;
    }

    public String getTaggedParagraph() {
        String sTagged = "";

        for (int i = 1; i <= this.igSentenceCount; ++i) {
            sTagged = sTagged + this.sentence[i].getTaggedSentence();
        }

        return sTagged;
    }

    public String getTranslatedParagraph() {
        String sTranslated = "";

        for (int i = 1; i <= this.igSentenceCount; ++i) {
            sTranslated = sTranslated + this.sentence[i].getTranslatedSentence();
        }

        return sTranslated;
    }

    public void recalculateParagraphSentimentScores() {
        for (int iSentence = 1; iSentence <= this.igSentenceCount; ++iSentence) {
            this.sentence[iSentence].recalculateSentenceSentimentScore();
        }

        this.calculateParagraphSentimentScores();
    }

    public void reClassifyClassifiedParagraphForSentimentChange(int iSentimentWordID) {
        if (this.igNegativeSentiment == 0) {
            this.calculateParagraphSentimentScores();
        } else {
            if (!this.bSentimentIDListMade) {
                this.makeSentimentIDList();
            }

            if (this.igSentimentIDListCount != 0) {
                if (Sort.i_FindIntPositionInSortedArray(iSentimentWordID, this.igSentimentIDList, 1, this.igSentimentIDListCount) >= 0) {
                    for (int iSentence = 1; iSentence <= this.igSentenceCount; ++iSentence) {
                        this.sentence[iSentence].reClassifyClassifiedSentenceForSentimentChange(iSentimentWordID);
                    }

                    this.calculateParagraphSentimentScores();
                }

            }
        }
    }

    public int getParagraphPositiveSentiment() {
        if (this.igPositiveSentiment == 0) {
            this.calculateParagraphSentimentScores();
        }

        return this.igPositiveSentiment;
    }

    public int getParagraphNegativeSentiment() {
        if (this.igNegativeSentiment == 0) {
            this.calculateParagraphSentimentScores();
        }

        return this.igNegativeSentiment;
    }

    public int getParagraphTrinarySentiment() {
        if (this.igNegativeSentiment == 0) {
            this.calculateParagraphSentimentScores();
        }

        return this.igTrinarySentiment;
    }

    public int getParagraphScaleSentiment() {
        if (this.igNegativeSentiment == 0) {
            this.calculateParagraphSentimentScores();
        }

        return this.igScaleSentiment;
    }

    private void calculateParagraphSentimentScores() {
        this.igPositiveSentiment = 1;
        this.igNegativeSentiment = -1;
        this.igTrinarySentiment = 0;
        if (this.options.bgExplainClassification && this.sgClassificationRationale.length() > 0) {
            this.sgClassificationRationale = "";
        }
        int iPosTotal = 0;
        int iPosMax = 0;
        int iNegTotal = 0;
        int iNegMax = 0;
        int iPosTemp = 0;
        int iNegTemp = 0;
        int[] iPos = new int[this.igSentenceCount + 1];
        int[] iNeg = new int[this.igSentenceCount + 1];
        int iSentencesUsed = 0;
        if (this.igSentenceCount != 0) {
            int iNegTot;
            for (iNegTot = 1; iNegTot <= this.igSentenceCount; ++iNegTot) {
                iPos[iNegTot] = this.sentence[iNegTot].getSentencePositiveSentiment();
                iNeg[iNegTot] = this.sentence[iNegTot].getSentenceNegativeSentiment();
                iPosTemp = iPos[iNegTot];
                iNegTemp = iNeg[iNegTot];
                boolean neutral = iPosTemp == 1 && iNegTemp == -1;
                //Where Trigger start to work
                if (options.bgLydian && !neutral && !this.sentence[iNegTot].isBgLydian()) {
                    iPosTemp = 1;
                    iNegTemp = -1;
                }
                if (iNegTemp != 0 || iPosTemp != 0) {
                    iNegTotal += iNegTemp;
                    ++iSentencesUsed;
                    if (iNegMax > iNegTemp) {
                        iNegMax = iNegTemp;
                    }
                    iPosTotal += iPosTemp;
                    if (iPosMax < iPosTemp) {
                        iPosMax = iPosTemp;
                    }
                }

                if (this.options.bgExplainClassification) {
                    this.sgClassificationRationale = this.sgClassificationRationale + this.sentence[iNegTot].getClassificationRationale() + " ";
                }

            }
            int var10000;
            if (iNegTotal == 0) {
                var10000 = this.options.igEmotionParagraphCombineMethod;
                this.options.getClass();
                if (var10000 != 2) {
                    this.igPositiveSentiment = 0;
                    this.igNegativeSentiment = 0;
                    this.igTrinarySentiment = this.binarySelectionTieBreaker();
                    return;
                }
            }
            var10000 = this.options.igEmotionParagraphCombineMethod;
            this.options.getClass();
            if (var10000 == 1) {
                this.igPositiveSentiment = (int) ((double) ((float) iPosTotal / (float) iSentencesUsed) + 0.5D);
                this.igNegativeSentiment = (int) ((double) ((float) iNegTotal / (float) iSentencesUsed) - 0.5D);
                if (this.options.bgExplainClassification) {
                    this.sgClassificationRationale = this.sgClassificationRationale + "[result = average (" + iPosTotal + " and " + iNegTotal + ") of " + iSentencesUsed + " sentences]";
                }
            } else {
                var10000 = this.options.igEmotionParagraphCombineMethod;
                this.options.getClass();
                if (var10000 == 2) {
                    this.igPositiveSentiment = iPosTotal;
                    this.igNegativeSentiment = iNegTotal;
                    if (this.options.bgExplainClassification) {
                        this.sgClassificationRationale = this.sgClassificationRationale + "[result: total positive; total negative]";
                    }
                } else {
                    this.igPositiveSentiment = iPosMax;
                    this.igNegativeSentiment = iNegMax;
                    if (this.options.bgExplainClassification) {
                        this.sgClassificationRationale = this.sgClassificationRationale + "[result: max + and - of any sentence]";
                    }
                }
            }

            var10000 = this.options.igEmotionParagraphCombineMethod;
            this.options.getClass();
            if (var10000 != 2) {
                if (this.igPositiveSentiment == 0) {
                    this.igPositiveSentiment = 1;
                }

                if (this.igNegativeSentiment == 0) {
                    this.igNegativeSentiment = -1;
                }
            }

            if (this.options.bgScaleMode) {
                this.igScaleSentiment = this.igPositiveSentiment + this.igNegativeSentiment;
                if (this.options.bgExplainClassification) {
                    this.sgClassificationRationale = this.sgClassificationRationale + "[scale result = sum of pos and neg scores]";
                }

            } else {
                var10000 = this.options.igEmotionParagraphCombineMethod;
                this.options.getClass();
                if (var10000 == 2) {
                    if (this.igPositiveSentiment == 0 && this.igNegativeSentiment == 0) {
                        if (this.options.bgBinaryVersionOfTrinaryMode) {
                            this.igTrinarySentiment = this.options.igDefaultBinaryClassification;
                            if (this.options.bgExplainClassification) {
                                this.sgClassificationRationale = this.sgClassificationRationale + "[binary result set to default value]";
                            }
                        } else {
                            this.igTrinarySentiment = 0;
                            if (this.options.bgExplainClassification) {
                                this.sgClassificationRationale = this.sgClassificationRationale + "[trinary result 0 as pos=1, neg=-1]";
                            }
                        }
                    } else {
                        if ((float) this.igPositiveSentiment > this.options.fgNegativeSentimentMultiplier * (float) (-this.igNegativeSentiment)) {
                            this.igTrinarySentiment = 1;
                            if (this.options.bgExplainClassification) {
                                this.sgClassificationRationale = this.sgClassificationRationale + "[overall result 1 as pos > -neg * " + this.options.fgNegativeSentimentMultiplier + "]";
                            }

                            return;
                        }

                        if ((float) this.igPositiveSentiment < this.options.fgNegativeSentimentMultiplier * (float) (-this.igNegativeSentiment)) {
                            this.igTrinarySentiment = -1;
                            if (this.options.bgExplainClassification) {
                                this.sgClassificationRationale = this.sgClassificationRationale + "[overall result -1 as pos < -neg * " + this.options.fgNegativeSentimentMultiplier + "]";
                            }

                            return;
                        }

                        if (this.options.bgBinaryVersionOfTrinaryMode) {
                            this.igTrinarySentiment = this.options.igDefaultBinaryClassification;
                            if (this.options.bgExplainClassification) {
                                this.sgClassificationRationale = this.sgClassificationRationale + "[binary result = default value as pos = -neg * " + this.options.fgNegativeSentimentMultiplier + "]";
                            }
                        } else {
                            this.igTrinarySentiment = 0;
                            if (this.options.bgExplainClassification) {
                                this.sgClassificationRationale = this.sgClassificationRationale + "[trinary result = 0 as pos = -neg * " + this.options.fgNegativeSentimentMultiplier + "]";
                            }
                        }
                    }
                } else {
                    if (this.igPositiveSentiment == 1 && this.igNegativeSentiment == -1) {
                        if (this.options.bgBinaryVersionOfTrinaryMode) {
                            this.igTrinarySentiment = this.binarySelectionTieBreaker();
                            if (this.options.bgExplainClassification) {
                                this.sgClassificationRationale = this.sgClassificationRationale + "[binary result = default value as pos=1 neg=-1]";
                            }
                        } else {
                            this.igTrinarySentiment = 0;
                            if (this.options.bgExplainClassification) {
                                this.sgClassificationRationale = this.sgClassificationRationale + "[trinary result = 0 as pos=1 neg=-1]";
                            }
                        }
                        return;
                    }
                    if (this.igPositiveSentiment > -this.igNegativeSentiment) {
                        this.igTrinarySentiment = 1;
                        if (this.options.bgExplainClassification) {
                            this.sgClassificationRationale = this.sgClassificationRationale + "[overall result = 1 as pos>-neg]";
                        }
                        return;
                    }

                    if (this.igPositiveSentiment < -this.igNegativeSentiment) {
                        this.igTrinarySentiment = -1;
                        if (this.options.bgExplainClassification) {
                            this.sgClassificationRationale = this.sgClassificationRationale + "[overall result = -1 as pos<-neg]";
                        }
                        return;
                    }

                    iNegTot = 0;
                    int iPosTot = 0;
                    for (int iSentence = 1; iSentence <= this.igSentenceCount; ++iSentence) {
                        iPosTot = iPos[iSentence];
                        iNegTot += iNeg[iSentence];
                    }
                    if (this.options.bgBinaryVersionOfTrinaryMode && iPosTot == -iNegTot) {
                        this.igTrinarySentiment = this.binarySelectionTieBreaker();
                        if (this.options.bgExplainClassification) {
                            this.sgClassificationRationale = this.sgClassificationRationale + "[binary result = default as posSentenceTotal>-negSentenceTotal]";
                        }
                    } else {
                        if (this.options.bgExplainClassification) {
                            this.sgClassificationRationale = this.sgClassificationRationale + "[overall result = largest of posSentenceTotal, negSentenceTotal]";
                        }
                        if (iPosTot > -iNegTot) {
                            this.igTrinarySentiment = 1;
                        } else {
                            this.igTrinarySentiment = -1;
                        }
                    }
                }
            }
        }
    }

    private int binarySelectionTieBreaker() {
        if (this.options.igDefaultBinaryClassification != 1 && this.options.igDefaultBinaryClassification != -1) {
            return this.generator.nextDouble() > 0.5D ? 1 : -1;
        } else {
            return this.options.igDefaultBinaryClassification != 1 && this.options.igDefaultBinaryClassification != -1 ? this.options.igDefaultBinaryClassification : this.options.igDefaultBinaryClassification;
        }
    }
}
