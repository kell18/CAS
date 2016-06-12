package cas;

import com.textocat.textokit.commons.cpe.FileDirectoryCollectionReader;
import com.textocat.textokit.commons.util.PipelineDescriptorUtils;
import com.textocat.textokit.morph.dictionary.MorphDictionaryAPIFactory;
import com.textocat.textokit.morph.fs.Word;
import com.textocat.textokit.morph.lemmatizer.LemmatizerAPI;
import com.textocat.textokit.postagger.MorphCasUtils;
import com.textocat.textokit.postagger.PosTaggerAPI;
import com.textocat.textokit.segmentation.SentenceSplitterAPI;
import com.textocat.textokit.tokenizer.TokenizerAPI;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ExternalResourceDescription;
import org.elasticsearch.common.regex.Regex;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextOKitExample {
    public static void main(String[] args)throws UIMAException, IOException {
        /*File inputDir = new File("C:\\Users\\Albert\\Code\\Scala\\CAS\\resources\\tmp");

        CollectionReaderDescription readerDesc = FileDirectoryCollectionReader.createDescription(inputDir);

        AnalysisEngineDescription aeDesc = createEngineDescription(
                createEngineDescription(TokenizerAPI.AE_TOKENIZER),
                createEngineDescription(SentenceSplitterAPI.AE_SENTENCE_SPLITTER),
                createEngineDescription(PosTaggerAPI.AE_POSTAGGER),
                createEngineDescription(LemmatizerAPI.AE_LEMMATIZER)
        );
        ExternalResourceDescription morphDictDesc =
                MorphDictionaryAPIFactory.getMorphDictionaryAPI().getResourceDescriptionForCachedInstance();
        morphDictDesc.setName(PosTaggerAPI.MORPH_DICTIONARY_RESOURCE_NAME);

        PipelineDescriptorUtils.getResourceManagerConfiguration(aeDesc)
                .addExternalResource(morphDictDesc);

        JCasIterable jCasIterable = new JCasIterable(readerDesc, aeDesc);
        for (JCas jCas : jCasIterable) {
            for (Word w : JCasUtil.select(jCas, Word.class)) {
                String src = w.getCoveredText();
                String lemma = MorphCasUtils.getFirstLemma(w);
                String posTag = MorphCasUtils.getFirstPosTag(w);
                System.out.print(String.format("%s/%s/%s ", src, lemma, posTag));
            }
            // mark the end of a document
            System.out.println("\n");
        }*/
    }
}
