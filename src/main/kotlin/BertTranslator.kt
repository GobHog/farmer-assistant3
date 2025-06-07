import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.djl.inference.Predictor
import ai.djl.ndarray.NDList
import ai.djl.nn.Activation
import ai.djl.repository.zoo.Criteria
import ai.djl.translate.Batchifier
import ai.djl.translate.Translator
import ai.djl.translate.TranslatorContext
import java.nio.file.Paths


// --- 1. Загрузка модели ---
fun loadTorchScriptModel(): Predictor<String, Float> {
    val criteria = Criteria.builder()
        .setTypes(String::class.java, Float::class.java)
        .optModelPath(Paths.get("resources/ml/ru_bert_traced.pt")) // путь к TorchScript-модели
        .optEngine("PyTorch")
        .optTranslator(BertTranslator())
        .build()

    val model = criteria.loadModel()
    return model.newPredictor()
}

// --- 2. Кастомный транслятор ---
class BertTranslator : Translator<String, Float> {
    private lateinit var tokenizer: HuggingFaceTokenizer

    override fun prepare(ctx: TranslatorContext) {
        tokenizer = HuggingFaceTokenizer.newInstance(Paths.get("tokenizer")) // путь к токенизатору HuggingFace
    }

    override fun processInput(ctx: TranslatorContext, input: String): NDList {
        val encoding = tokenizer.encode(input)
        val manager = ctx.ndManager

        val idsLongArray = encoding.ids.map { it.toLong() }.toLongArray()
        val inputIds = manager.create(idsLongArray)
        val attentionMaskLongArray = encoding.attentionMask.map { it.toLong() }.toLongArray()
        val attentionMask = manager.create(attentionMaskLongArray)


        return NDList(inputIds.expandDims(0), attentionMask.expandDims(0))
    }

    override fun processOutput(ctx: TranslatorContext, list: NDList): Float {
        val logits = list[0]
        val probabilities = Activation.sigmoid(logits)
        val score = probabilities[0].getFloat() // Преобразуем logits -> probability
        return score
    }

    override fun getBatchifier(): Batchifier? = null
}