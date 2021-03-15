package preprocess;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;
public class PreProcess {
	private static String path= "data/sentistrength_data/";
	private static Map<String, String> modifiedTermsMap;
	private static ArrayList<String> listOfEmoticon;
	private boolean bgPreprocess;
	private FileUtility objFileUtility;
	private ArrayList<String> bctSentenceArr;
	private int bctArrSize;
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
	public boolean isBgPreprocess() {
		return bgPreprocess;
	}
	public void setBgPreprocess(boolean bgPreprocess) {
		this.bgPreprocess = bgPreprocess;
	}
	public void init() {
		if(objFileUtility==null) {
			objFileUtility = new FileUtility();
			objFileUtility.getObjPOSTagging().init();
		}
	}
	public void setModifiedTermsMap(Map<String, String> mtm) {
    	if(modifiedTermsMap==null) {
    		modifiedTermsMap=mtm;
    	}
    }
	public ArrayList<String> getProcessedArray(String text) throws Exception {
		listOfEmoticon=objFileUtility.GetEmoTerms(path+"EmoticonLookupTable.txt");
		FileUtility.setListOfEmoticon(listOfEmoticon);
		if(judeFileExists(path+"ModifiedTermsLookupTable.txt")) {
			setModifiedTermsMap(objFileUtility.GetModifiedTermsMap(path+"ModifiedTermsLookupTable.txt"));
		}
		// inlet of Preprocess & Segment
		ArrayList<String> processedArray=(ArrayList<String>)objFileUtility.GetProcessedArray(text,null,bgPreprocess,modifiedTermsMap);
		ArrayList<String> quotedArray=dealWithQuote(processedArray);
		if(objFileUtility.getBctArrSize()!=0) {
			ArrayList<String> bctSentenceArr=dealWithQuote(objFileUtility.getBctSentenceArr());
			setBctSentenceArr(bctSentenceArr);
			setBctArrSize(bctSentenceArr.size());
		}
		return conjArray(quotedArray);
	}
	
	public ArrayList<String> conjArray(ArrayList<String> arr){
		if(arr.size()<=1) {
			return arr;
		}
		Pattern p=Pattern.compile("[a-zA-z]");
		ArrayList<String> conjed=new ArrayList<String>();
		String product=arr.get(0).trim();
		int start=1;
		if(!isEmoticon(product) && !p.matcher(product).find()) {
			start++;
			product=arr.get(1).trim();
		}
		String part=null;
		for(int i=start;i<arr.size();i++) {
			part=arr.get(i).trim();
			if(isEmoticon(part)) {
				conjed.add(product);
				product=part;
			}else {
				if(p.matcher(part).find()) {
					conjed.add(product);
					product=part;
				}else {
					product+=part;
				}
			}
		}
		conjed.add(product);
		return conjed;
	} 
	
	public static void readArray(ArrayList<String> list) {
		for(int i = 0 ; i<list.size();i++){
			System.out.println(list.get(i));
		}
    }
	
	private boolean isEmoticon(String word) {
		for (String emoticon :listOfEmoticon) {
			if (word.contains(emoticon)) {
				return true;
	        } 
	    } 
		return false;
	}
	
	private ArrayList<String> dealWithQuote(ArrayList<String> arr){
		ArrayList<String> dealed=new ArrayList<String>();
		for(int i=0;i<arr.size();i++) {
			String text=arr.get(i);
			if (text.indexOf("\"")!=-1) {
					text=text.replaceAll("\"", "'");
			}
			dealed.add(text);
		}
		return dealed;
	}
	
	public boolean judeFileExists(String filePath) {
		  File file = new File(filePath);
		  if (file.exists()) {
	          return true;
	      } else {
	    	  return false;
	      }

	  }

}
