/*
 * Copyright 2017 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.iceberg.spark;

import com.google.common.collect.Lists;
import com.netflix.iceberg.Schema;
import com.netflix.iceberg.types.Type;
import com.netflix.iceberg.types.TypeUtil;
import com.netflix.iceberg.types.Types;
import org.apache.spark.sql.types.ArrayType$;
import org.apache.spark.sql.types.BinaryType$;
import org.apache.spark.sql.types.BooleanType$;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DateType$;
import org.apache.spark.sql.types.DecimalType$;
import org.apache.spark.sql.types.DoubleType$;
import org.apache.spark.sql.types.FloatType$;
import org.apache.spark.sql.types.IntegerType$;
import org.apache.spark.sql.types.LongType$;
import org.apache.spark.sql.types.MapType$;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StringType$;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType$;
import org.apache.spark.sql.types.TimestampType$;
import java.util.List;

class TypeToSparkType extends TypeUtil.SchemaVisitor<DataType> {
  TypeToSparkType() {
  }

  @Override
  public DataType schema(Schema schema, DataType structType) {
    return structType;
  }

  @Override
  public DataType struct(Types.StructType struct, List<DataType> fieldResults) {
    List<Types.NestedField> fields = struct.fields();

    List<StructField> sparkFields = Lists.newArrayListWithExpectedSize(fieldResults.size());
    for (int i = 0; i < fields.size(); i += 1) {
      Types.NestedField field = fields.get(i);
      DataType type = fieldResults.get(i);
      sparkFields.add(StructField.apply(field.name(), type, field.isOptional(), Metadata.empty()));
    }

    return StructType$.MODULE$.apply(sparkFields);
  }

  @Override
  public DataType field(Types.NestedField field, DataType fieldResult) {
    return fieldResult;
  }

  @Override
  public DataType list(Types.ListType list, DataType elementResult) {
    return ArrayType$.MODULE$.apply(elementResult, list.isElementOptional());
  }

  @Override
  public DataType map(Types.MapType map, DataType valueResult) {
    return MapType$.MODULE$.apply(StringType$.MODULE$, valueResult, map.isValueOptional());
  }

  @Override
  public DataType primitive(Type.PrimitiveType primitive) {
    switch (primitive.typeId()) {
      case BOOLEAN:
        return BooleanType$.MODULE$;
      case INTEGER:
        return IntegerType$.MODULE$;
      case LONG:
        return LongType$.MODULE$;
      case FLOAT:
        return FloatType$.MODULE$;
      case DOUBLE:
        return DoubleType$.MODULE$;
      case DATE:
        return DateType$.MODULE$;
      case TIME:
        throw new UnsupportedOperationException(
            "Spark does not support time fields");
      case TIMESTAMP:
        Types.TimestampType timestamp = (Types.TimestampType) primitive;
        if (timestamp.shouldAdjustToUTC()) {
          return TimestampType$.MODULE$;
        }
        throw new UnsupportedOperationException(
            "Spark does not support timestamp without time zone fields");
      case STRING:
        return StringType$.MODULE$;
      case UUID:
        // use String
        return StringType$.MODULE$;
      case FIXED:
        return BinaryType$.MODULE$;
      case BINARY:
        return BinaryType$.MODULE$;
      case DECIMAL:
        Types.DecimalType decimal = (Types.DecimalType) primitive;
        return DecimalType$.MODULE$.apply(decimal.precision(), decimal.scale());
      default:
        throw new UnsupportedOperationException(
            "Cannot convert unknown type to Spark: " + primitive);
    }
  }
}
