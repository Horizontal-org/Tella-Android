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
    _id = _id,
    id = id,
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
    values = values?.map { it.mapToDomainModel() } ?: emptyList())

fun ValueEntity.mapToDomainModel() = SelectValue(
    _id = _id,
    id = id,
    label = label,
)
