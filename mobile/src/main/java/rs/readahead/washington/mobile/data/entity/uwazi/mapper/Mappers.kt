package rs.readahead.washington.mobile.data.entity.uwazi.mapper

import rs.readahead.washington.mobile.data.entity.uwazi.*
import rs.readahead.washington.mobile.domain.entity.uwazi.*

/**
 * Mapper for template response
 */
fun TemplateResponse.mapToDomainModel() = rows?.map {
    it.mapToDomainModel()
} ?: emptyList()

fun UwaziEntityRow.mapToDomainModel() = UwaziRow(
    version = __v,
    _id = _id,
    commonProperties = commonProperties?.map { it.mapToDomainModel() } ?: emptyList(),
    default = default,
    name = name,
    properties = properties?.map { it.mapToDomainModel() } ?: emptyList())

fun CommonPropertyEntity.mapToDomainModel() = CommonProperty(
    id = _id,
    generatedId = generatedId,
    isCommonProperty = isCommonProperty,
    label = label,
    localID = localID?: "",
    name = name,
    prioritySorting = prioritySorting,
    type = type
)

fun PropertyEntity.mapToDomainModel() = Property(
    _id = _id ?: "",
    id = id ?: "",
    showInCard = showInCard,
    content = content?: "",
    label = label?:"",
    name = name?:"",
    nestedProperties = nestedProperties?: emptyList(),
    required = required,
    type = type
)


/**
 * Mapper for dictionary response
 */
fun DictionaryResponse.mapToDomainModel() = rows?.map {
    it.mapToDomainModel()
} ?: emptyList()

fun RowDictionaryEntity.mapToDomainModel() = RowDictionary(
    version = __v ?: 0,
    _id = _id ?: "",
    name = name ?: "",
    values = mapToDomainModel(values)
)

fun ValueEntity.mapToDomainModel() = SelectValue(
    _id = _id?: "",
    id = id?: "",
    label = label
)

fun ValueEntity.mapToDomainModel(valueEntities : List<ValueEntity>) = SelectValue(
    _id = _id?: "",
    id = id?: "",
    label = label,
    values = mapNestedToDomainModel(valueEntities)
)

fun mapToDomainModel(values : List<ValueEntity>?) : List<SelectValue> {
    if (values.isNullOrEmpty() ) return emptyList()

    val resultList: List<SelectValue> = if (values[0].values.isNullOrEmpty()){
        values.map {
            it.mapToDomainModel()
        }
    } else {
        arrayListOf(values[0].mapToDomainModel(values))
    }
    return resultList
}


fun mapNestedToDomainModel(values : List<ValueEntity>) :Map<String,List<NestedSelectValue>> {
    val resultList = hashMapOf<String,List<NestedSelectValue>>()

    values.forEach { valueEntity ->
        resultList[valueEntity.label] = valueEntity.values?.map { it.mapToDomainModel() } ?: emptyList()
    }

    return resultList
}


fun NestedValueEntity.mapToDomainModel() = NestedSelectValue (
    id = id?: "",
    label = label?: "")

