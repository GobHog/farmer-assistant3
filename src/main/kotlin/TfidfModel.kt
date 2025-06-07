import kotlinx.serialization.Serializable

@Serializable
data class TfidfModel(val vocabulary: Map<String, Int>,
                      val idf: List<Double>,
                      val weights: List<Double>,
                      val bias: Double)
