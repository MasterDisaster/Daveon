package org.apache.lucene.analysis.icu.segmentation;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cjk.CJKBigramFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.icu.ICUNormalizer2Filter;
import org.apache.lucene.analysis.util.CharArraySet;

/**
 * Tests ICUTokenizer's ability to work with CJKBigramFilter.
 * Most tests adopted from TestCJKTokenizer
 */
public class TestWithCJKBigramFilter extends BaseTokenStreamTestCase {
  
  /**
   * ICUTokenizer+CJKBigramFilter
   */
  private Analyzer analyzer = new Analyzer() {
    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer source = new ICUTokenizer(reader);
      TokenStream result = new CJKBigramFilter(source);
      return new TokenStreamComponents(source, new StopFilter(TEST_VERSION_CURRENT, result, CharArraySet.EMPTY_SET));
    }
  };
  
  /**
   * ICUTokenizer+ICUNormalizer2Filter+CJKBigramFilter.
   * 
   * ICUNormalizer2Filter uses nfkc_casefold by default, so this is a language-independent
   * superset of CJKWidthFilter's foldings.
   */
  private Analyzer analyzer2 = new Analyzer() {
    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer source = new ICUTokenizer(reader);
      // we put this before the CJKBigramFilter, because the normalization might combine
      // some halfwidth katakana forms, which will affect the bigramming.
      TokenStream result = new ICUNormalizer2Filter(source);
      result = new CJKBigramFilter(source);
      return new TokenStreamComponents(source, new StopFilter(TEST_VERSION_CURRENT, result, CharArraySet.EMPTY_SET));
    }
  };
  
  public void testJa1() throws IOException {
    assertAnalyzesTo(analyzer, "??????????????????????????????",
      new String[] { "??????", "??????", "??????", "??????", "??????", "??????", "??????", "??????", "??????" },
      new int[] { 0, 1, 2, 3, 4, 5, 6, 7,  8 },
      new int[] { 2, 3, 4, 5, 6, 7, 8, 9, 10 },
      new String[] { "<DOUBLE>", "<DOUBLE>", "<DOUBLE>", "<DOUBLE>", "<DOUBLE>", "<DOUBLE>", "<DOUBLE>", "<DOUBLE>", "<DOUBLE>" },
      new int[] { 1, 1, 1, 1, 1, 1, 1, 1,  1 });
  }
  
  public void testJa2() throws IOException {
    assertAnalyzesTo(analyzer, "??? ????????? ??????????????? ???",
      new String[] { "???", "??????", "??????", "??????", "??????", "??????", "??????", "???" },
      new int[] { 0, 2, 3, 6, 7,  8,  9, 12 },
      new int[] { 1, 4, 5, 8, 9, 10, 11, 13 },
      new String[] { "<SINGLE>", "<DOUBLE>", "<DOUBLE>", "<DOUBLE>", "<DOUBLE>", "<DOUBLE>", "<DOUBLE>", "<SINGLE>" },
      new int[] { 1, 1, 1, 1, 1,  1,  1,  1 });
  }
  
  public void testC() throws IOException {
    assertAnalyzesTo(analyzer, "abc defgh ijklmn opqrstu vwxy z",
      new String[] { "abc", "defgh", "ijklmn", "opqrstu", "vwxy", "z" },
      new int[] { 0, 4, 10, 17, 25, 30 },
      new int[] { 3, 9, 16, 24, 29, 31 },
      new String[] { "<ALPHANUM>", "<ALPHANUM>", "<ALPHANUM>", "<ALPHANUM>", "<ALPHANUM>", "<ALPHANUM>" },
      new int[] { 1, 1,  1,  1,  1,  1 });
  }
  
  /**
   * LUCENE-2207: wrong offset calculated by end() 
   */
  public void testFinalOffset() throws IOException {
    assertAnalyzesTo(analyzer, "??????",
      new String[] { "??????" },
      new int[] { 0 },
      new int[] { 2 },
      new String[] { "<DOUBLE>" },
      new int[] { 1 });
    
    assertAnalyzesTo(analyzer, "??????   ",
      new String[] { "??????" },
      new int[] { 0 },
      new int[] { 2 },
      new String[] { "<DOUBLE>" },
      new int[] { 1 });

    assertAnalyzesTo(analyzer, "test",
      new String[] { "test" },
      new int[] { 0 },
      new int[] { 4 },
      new String[] { "<ALPHANUM>" },
      new int[] { 1 });
    
    assertAnalyzesTo(analyzer, "test   ",
      new String[] { "test" },
      new int[] { 0 },
      new int[] { 4 },
      new String[] { "<ALPHANUM>" },
      new int[] { 1 });
    
    assertAnalyzesTo(analyzer, "??????test",
      new String[] { "??????", "test" },
      new int[] { 0, 2 },
      new int[] { 2, 6 },
      new String[] { "<DOUBLE>", "<ALPHANUM>" },
      new int[] { 1, 1 });
    
    assertAnalyzesTo(analyzer, "test??????    ",
      new String[] { "test", "??????" },
      new int[] { 0, 4 },
      new int[] { 4, 6 },
      new String[] { "<ALPHANUM>", "<DOUBLE>" },
      new int[] { 1, 1 });
  }
  
  public void testMix() throws IOException {
    assertAnalyzesTo(analyzer, "???????????????abc???????????????",
      new String[] { "??????", "??????", "??????", "??????", "abc", "??????", "??????", "??????", "??????" },
      new int[] { 0, 1, 2, 3, 5,  8,  9, 10, 11 },
      new int[] { 2, 3, 4, 5, 8, 10, 11, 12, 13 },
      new String[] { "<DOUBLE>", "<DOUBLE>", "<DOUBLE>", "<DOUBLE>", "<ALPHANUM>", "<DOUBLE>", "<DOUBLE>", "<DOUBLE>", "<DOUBLE>" },
      new int[] { 1, 1, 1, 1, 1,  1,  1,  1,  1});
  }
  
  public void testMix2() throws IOException {
    assertAnalyzesTo(analyzer, "???????????????ab???c???????????? ???",
      new String[] { "??????", "??????", "??????", "??????", "ab", "???", "c", "??????", "??????", "??????", "???" },
      new int[] { 0, 1, 2, 3, 5, 7, 8,  9, 10, 11, 14 },
      new int[] { 2, 3, 4, 5, 7, 8, 9, 11, 12, 13, 15 },
      new String[] { "<DOUBLE>", "<DOUBLE>", "<DOUBLE>", "<DOUBLE>", "<ALPHANUM>", "<SINGLE>", "<ALPHANUM>", "<DOUBLE>", "<DOUBLE>", "<DOUBLE>", "<SINGLE>" },
      new int[] { 1, 1, 1, 1, 1, 1, 1,  1,  1,  1,  1 });
  }
  
  /**
   * Non-english text (outside of CJK) is treated normally, according to unicode rules 
   */
  public void testNonIdeographic() throws IOException {
    assertAnalyzesTo(analyzer, "??? ?????????? ????????",
        new String[] { "???", "??????????", "????????" },
        new int[] { 0, 2, 8 },
        new int[] { 1, 7, 12 },
        new String[] { "<SINGLE>", "<ALPHANUM>", "<ALPHANUM>" },
        new int[] { 1, 1, 1 });
  }
  
  /**
   * Same as the above, except with a nonspacing mark to show correctness.
   */
  public void testNonIdeographicNonLetter() throws IOException {
    assertAnalyzesTo(analyzer, "??? ???????????? ????????",
        new String[] { "???", "????????????", "????????" },
        new int[] { 0, 2, 9 },
        new int[] { 1, 8, 13 },
        new String[] { "<SINGLE>", "<ALPHANUM>", "<ALPHANUM>" },
        new int[] { 1, 1, 1 });
  }
  
  public void testSurrogates() throws IOException {
    assertAnalyzesTo(analyzer, "???????????????????",
      new String[] { "???????", "??????", "??????", "??????", "??????" },
      new int[] { 0, 2, 3, 4, 5 },
      new int[] { 3, 4, 5, 6, 7 },
      new String[] { "<DOUBLE>", "<DOUBLE>", "<DOUBLE>", "<DOUBLE>", "<DOUBLE>" },
      new int[] { 1, 1, 1, 1, 1 });
  }
  
  public void testReusableTokenStream() throws IOException {
    assertAnalyzesToReuse(analyzer, "???????????????abc???????????????",
        new String[] { "??????", "??????", "??????", "??????", "abc", "??????", "??????", "??????", "??????" },
        new int[] { 0, 1, 2, 3, 5,  8,  9, 10, 11 },
        new int[] { 2, 3, 4, 5, 8, 10, 11, 12, 13 },
        new String[] { "<DOUBLE>", "<DOUBLE>", "<DOUBLE>", "<DOUBLE>", "<ALPHANUM>", "<DOUBLE>", "<DOUBLE>", "<DOUBLE>", "<DOUBLE>" },
        new int[] { 1, 1, 1, 1, 1,  1,  1,  1,  1});
    
    assertAnalyzesToReuse(analyzer, "???????????????ab???c???????????? ???",
        new String[] { "??????", "??????", "??????", "??????", "ab", "???", "c", "??????", "??????", "??????", "???" },
        new int[] { 0, 1, 2, 3, 5, 7, 8,  9, 10, 11, 14 },
        new int[] { 2, 3, 4, 5, 7, 8, 9, 11, 12, 13, 15 },
        new String[] { "<DOUBLE>", "<DOUBLE>", "<DOUBLE>", "<DOUBLE>", "<ALPHANUM>", "<SINGLE>", "<ALPHANUM>", "<DOUBLE>", "<DOUBLE>", "<DOUBLE>", "<SINGLE>" },
        new int[] { 1, 1, 1, 1, 1, 1, 1,  1,  1,  1,  1 });
  }
  
  public void testSingleChar() throws IOException {
    assertAnalyzesTo(analyzer, "???",
      new String[] { "???" },
      new int[] { 0 },
      new int[] { 1 },
      new String[] { "<SINGLE>" },
      new int[] { 1 });
  }
  
  public void testTokenStream() throws IOException {
    assertAnalyzesTo(analyzer, "?????????", 
      new String[] { "??????", "??????"},
      new int[] { 0, 1 },
      new int[] { 2, 3 },
      new String[] { "<DOUBLE>", "<DOUBLE>" },
      new int[] { 1, 1 });
  }
}
