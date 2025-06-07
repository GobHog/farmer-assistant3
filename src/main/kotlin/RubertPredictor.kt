import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.djl.translate.Translator
import ai.djl.translate.TranslatorContext
import ai.djl.translate.Batchifier
import ai.djl.ndarray.NDList
import ai.djl.Model
import ai.djl.inference.Predictor
import ai.djl.Device
import java.nio.file.Path
import kotlin.math.exp

class RubertPredictor(modelPath: Path) {

    private val predictor: Predictor<String, Float>

    init {
        val model = Model.newInstance("ruBERT", Device.cpu())
        model.load(modelPath)

        val tokenizer = HuggingFaceTokenizer.newInstance("cointegrated/rubert-tiny")

        predictor = model.newPredictor(object : Translator<String, Float> {
            override fun processInput(ctx: TranslatorContext, input: String): NDList {
                val encoding = tokenizer.encode(input)
                val inputIds = ctx.ndManager.create(encoding.ids.map { it.toLong() }.toLongArray())
                val attentionMask = ctx.ndManager.create(encoding.attentionMask.map { it.toLong() }.toLongArray())

                val inputIdsExpanded = inputIds.expandDims(0)
                val attentionMaskExpanded = attentionMask.expandDims(0)

                return NDList(inputIdsExpanded, attentionMaskExpanded)
            }

            override fun processOutput(ctx: TranslatorContext, output: NDList): Float {
                val logits = output[0]
                return sigmoid(logits.toFloatArray()[0])
            }

            override fun getBatchifier(): Batchifier = Batchifier.STACK
        })
    }

    fun predict(text: String): Float {
        return predictor.predict(text)
    }

    private fun sigmoid(x: Float): Float = (1f / (1f + exp(-x)))
}
