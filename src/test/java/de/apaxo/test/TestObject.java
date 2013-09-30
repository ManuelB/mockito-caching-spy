package de.apaxo.test;

/**
 * Test object which holds text.
 * 
 * @author Manue Blechschmidt <blechschmidt@apaxo.de>
 * 
 */
public class TestObject {

    private String text;
    private int    countOfAnswers = 0;

    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * @param text
     *            the text to set
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Returns the string that was provided as input including a counter.
     * 
     * @param echo
     * @return
     */
    public String echo(String echo) {
        return echo + countOfAnswers++;
    }

    /**
     * Returns 5.
     * 
     * @return
     */
    public int five() {
        return 5;
    }

    /**
     * Returns 4.
     * 
     * @return
     */
    public int four() {
        return 4;
    }

    /**
     * This is for testing the fileName feature.
     * 
     * @param fileName
     * @return
     */
    public String fileNameAsParameter(String fileName) {
        return fileName;
    }
}
