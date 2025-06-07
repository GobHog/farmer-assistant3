import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.djl.modality.nlp.bert.BertTokenizer
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.LongBuffer
import java.nio.file.Path
import kotlin.math.exp

class OnnxRubertPredictor(modelPath: Path) {
    private val env = OrtEnvironment.getEnvironment()
    private val session: OrtSession = env.createSession(modelPath.toString(), OrtSession.SessionOptions())

    // Используем DJL HuggingFaceTokenizer вместо BertTokenizer.load()
    private val tokenizer = HuggingFaceTokenizer.newInstance("cointegrated/rubert-tiny2")

    fun predict(text: String): Float {
        // Токенизация
        val encoding = tokenizer.encode(text)
        val inputIds = encoding.ids.map { it.toLong() }.toLongArray()
        val attentionMask = encoding.attentionMask.map { it.toLong() }.toLongArray()


        val shape = longArrayOf(1, inputIds.size.toLong())

        val inputIdTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds), shape)
        val attentionMaskTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), shape)

        val inputs = mapOf(
            "input_ids" to inputIdTensor,
            "attention_mask" to attentionMaskTensor
        )

        val results = session.run(inputs)
        val logits = (results[0].value as Array<FloatArray>)[0]

        val probs = softmax(logits)
        return probs[1] // вероятность класса 1 (агро)
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val exps = logits.map { exp(it.toDouble()) }
        val sum = exps.sum()
        return exps.map { (it / sum).toFloat() }.toFloatArray()
    }
}