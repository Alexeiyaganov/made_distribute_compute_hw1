
package org.apache.spark.ml.made

import org.apache.spark.ml.linalg.{DenseVector, Vector, VectorUDT, Vectors}
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.param.shared.{HasInputCol, HasOutputCol}
import org.apache.spark.ml.stat.Summarizer
import org.apache.spark.ml.util.{DefaultParamsReadable, DefaultParamsWritable, Identifiable, SchemaUtils}
import org.apache.spark.ml.{Estimator, Model}
import org.apache.spark.sql.{DataFrame, Dataset, Encoder, Row}
import org.apache.spark.sql.types.StructType

trait StandardScalerParams extends HasInputCol with HasOutputCol {
  def setInputCol(value: String) : this.type = set(inputCol, value)
  def setOutputCol(value: String): this.type = set(outputCol, value)

  protected def validateAndTransformSchema(schema: StructType): StructType = {
    SchemaUtils.checkColumnType(schema, getInputCol, new VectorUDT())

    if (schema.fieldNames.contains($(outputCol))) {
      SchemaUtils.checkColumnType(schema, getOutputCol, new VectorUDT())
      schema
    } else {
      SchemaUtils.appendColumn(schema, schema(getInputCol).copy(name = getOutputCol))
    }
  }
}

class StandardScaler(override val uid: String) extends Estimator[StandardScalerModel] with StandardScalerParams
  with DefaultParamsWritable {

  def this() = this(Identifiable.randomUID("standardScaler"))

  override def fit(dataset: Dataset[_]): StandardScalerModel = {
    val Row(Row(mean, std)) = dataset
      .select(Summarizer.metrics("mean", "std").summary(dataset($(inputCol))))
      .first()

    copyValues(new StandardScalerModel(mean.asInstanceOf[Vector].toDense, std.asInstanceOf[Vector].toDense)).setParent(this)
  }

  override def copy(extra: ParamMap): Estimator[StandardScalerModel] = ???

  override def transformSchema(schema: StructType): StructType = validateAndTransformSchema(schema)
}

object StandardScaler extends DefaultParamsReadable[StandardScaler]

class StandardScalerModel private[made](
                                         override val uid: String,
                                         val means: DenseVector,
                                         val stds: DenseVector) extends Model[StandardScalerModel] with StandardScalerParams {

  private[made] def this(means: DenseVector, stds: DenseVector) =
    this(Identifiable.randomUID("standardScalerModel"), means, stds)

  override def copy(extra: ParamMap): StandardScalerModel = ???

  override def transform(dataset: Dataset[_]): DataFrame = {
    val transformUdf = dataset.sqlContext.udf.register(uid + "_transform",
      (x : Vector) => {
        Vectors.fromBreeze((x.asBreeze - means.asBreeze) /:/ stds.asBreeze)
      })

    dataset.withColumn($(outputCol), transformUdf(dataset($(inputCol))))
  }

  override def transformSchema(schema: StructType): StructType = validateAndTransformSchema(schema)
}
