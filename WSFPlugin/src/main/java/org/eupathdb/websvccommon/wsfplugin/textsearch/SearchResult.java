/**
 * 
 */
package org.eupathdb.websvccommon.wsfplugin.textsearch;

/**
 * @author John I
 * @created Nov 16, 2008
 */
public class SearchResult implements Comparable <SearchResult> {


    private String sourceId;
    private String projectId;
    private float maxScore; 
    private StringBuilder fieldsMatched;

    public SearchResult(String sourceId, String projectId, float maxScore, String fieldsMatched) {
	this.sourceId = sourceId;
	this.maxScore = maxScore;
	this.projectId = projectId;
	this.fieldsMatched = new StringBuilder(fieldsMatched);
    }

    public float getMaxScore() {
	return maxScore;
    }
    
    public void setMaxScore(float maxScore) {
      this.maxScore = maxScore;
    }

    public String getSourceId() {
	return sourceId;
    }

    public void setSourceId(String id) {
	sourceId = id;
    }

    public String getProjectId() {
	return projectId;
    }
    
    public String getGeneSourceId() {
      return null;
      }

    public String getPrimaryId() {
      return sourceId;
    }

    public void setProjectId(String id) {
	projectId = id;
    }

    public String getFieldsMatched() {
	return fieldsMatched.toString();
    }

    public void combine(SearchResult other) {
	if (other.getMaxScore() > maxScore) {
	    maxScore = other.getMaxScore();
	    fieldsMatched.insert(0, ", ").insert(0, other.fieldsMatched);
	} else {
	    //	    fieldsMatched.append(other.getFieldsMatched()), if fieldsMatched were a StringBuffer
	    fieldsMatched.append(",").append(other.fieldsMatched);
	}
    }

    @Override
    public int compareTo(SearchResult other) {
      if (other.getMaxScore() > maxScore || (other.getMaxScore() == maxScore && sourceId.compareTo(other.getSourceId()) < 0)) {
        return -1;
	    // the next match condition is redundant with the else clause.
        // } else if(other.getMaxScore() < maxScore || (other.getMaxScore() == maxScore && sourceId.compareTo(other.getSourceId()) > 0)) {
        // return 1;
      } else {
        return 1;
      }
    }
}
