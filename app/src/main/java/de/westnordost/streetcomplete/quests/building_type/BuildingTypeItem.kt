package de.westnordost.streetcomplete.quests.building_type

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.quests.building_type.BuildingType.*
import de.westnordost.streetcomplete.quests.building_type.BuildingType.BARN
import de.westnordost.streetcomplete.quests.building_type.BuildingType.COWSHED
import de.westnordost.streetcomplete.quests.building_type.BuildingType.DIGESTER
import de.westnordost.streetcomplete.quests.building_type.BuildingType.PRESBYTERY
import de.westnordost.streetcomplete.quests.building_type.BuildingType.RIDING_HALL
import de.westnordost.streetcomplete.quests.building_type.BuildingType.SPORTS_HALL
import de.westnordost.streetcomplete.quests.building_type.BuildingType.STABLE
import de.westnordost.streetcomplete.quests.building_type.BuildingType.STY
import de.westnordost.streetcomplete.quests.building_type.BuildingType.TRANSIT_SHELTER
import de.westnordost.streetcomplete.quests.building_type.BuildingType.TRANSFORMER_TOWER
import de.westnordost.streetcomplete.view.image_select.GroupableDisplayItem
import de.westnordost.streetcomplete.view.image_select.Item

fun List<BuildingType>.toItems() = mapNotNull { it.asItem() }

fun BuildingType.asItem(): GroupableDisplayItem<BuildingType>? {
    val iconResId = iconResId ?: return null
    val titleResId = titleResId ?: return null
    return Item(this, iconResId, titleResId, descriptionResId)
}

private val BuildingType.titleResId: Int? get() = when (this) {
    HOUSE ->           R.string.quest_buildingType_house
    APARTMENTS ->      R.string.quest_buildingType_apartments
    DETACHED ->        R.string.quest_buildingType_detached
    SEMI_DETACHED ->   R.string.quest_buildingType_semi_detached
    TERRACE ->         R.string.quest_buildingType_terrace2
    HOTEL ->           R.string.quest_buildingType_hotel
    DORMITORY ->       R.string.quest_buildingType_dormitory
    HOUSEBOAT ->       R.string.quest_buildingType_houseboat
    BUNGALOW ->        R.string.quest_buildingType_bungalow
    STATIC_CARAVAN ->  R.string.quest_buildingType_static_caravan
    HUT ->             R.string.quest_buildingType_hut
    INDUSTRIAL ->      R.string.quest_buildingType_industrial
    RETAIL ->          R.string.quest_buildingType_retail
    OFFICE ->          R.string.quest_buildingType_office
    WAREHOUSE ->       R.string.quest_buildingType_warehouse
    KIOSK ->           R.string.quest_buildingType_kiosk
    STORAGE_TANK ->    R.string.quest_buildingType_storage_tank
    KINDERGARTEN ->    R.string.quest_buildingType_kindergarten
    SCHOOL ->          R.string.quest_buildingType_school
    COLLEGE ->         R.string.quest_buildingType_college
    SPORTS_CENTRE ->   R.string.quest_buildingType_sports_centre
    HOSPITAL ->        R.string.quest_buildingType_hospital
    STADIUM ->         R.string.quest_buildingType_stadium
    GRANDSTAND ->      R.string.quest_buildingType_grandstand
    TRAIN_STATION ->   R.string.quest_buildingType_train_station
    TRANSIT_SHELTER -> R.string.quest_buildingType_transit_shelter
    TRANSPORTATION ->  R.string.quest_buildingType_transportation
    FIRE_STATION ->    R.string.quest_buildingType_fire_station
    UNIVERSITY ->      R.string.quest_buildingType_university
    GOVERNMENT ->      R.string.quest_buildingType_government
    CHURCH ->          R.string.quest_buildingType_church
    CHAPEL ->          R.string.quest_buildingType_chapel
    CATHEDRAL ->       R.string.quest_buildingType_cathedral
    MOSQUE ->          R.string.quest_buildingType_mosque
    TEMPLE ->          R.string.quest_buildingType_temple
    PAGODA ->          R.string.quest_buildingType_pagoda
    SYNAGOGUE ->       R.string.quest_buildingType_synagogue
    SHRINE ->          R.string.quest_buildingType_shrine
    CARPORT ->         R.string.quest_buildingType_carport
    GARAGE ->          R.string.quest_buildingType_garage
    GARAGES ->         R.string.quest_buildingType_garages
    PARKING ->         R.string.quest_buildingType_parking
    FARM ->            R.string.quest_buildingType_farmhouse
    FARM_AUXILIARY ->  R.string.quest_buildingType_farm_auxiliary
    SILO ->            R.string.quest_buildingType_silo
    GREENHOUSE ->      R.string.quest_buildingType_greenhouse
    SHED ->            R.string.quest_buildingType_shed
    ALLOTMENT_HOUSE -> R.string.quest_buildingType_allotment_house
    ROOF ->            R.string.quest_buildingType_roof
    BRIDGE ->          R.string.quest_buildingType_bridge
    TOILETS ->         R.string.quest_buildingType_toilets
    SERVICE ->         R.string.quest_buildingType_service
    HANGAR ->          R.string.quest_buildingType_hangar
    BUNKER ->          R.string.quest_buildingType_bunker
    BOATHOUSE ->       R.string.quest_buildingType_boathouse
    HISTORIC ->        R.string.quest_buildingType_historic
    ABANDONED ->       R.string.quest_buildingType_abandoned
    RUINS ->           R.string.quest_buildingType_ruins
    RESIDENTIAL ->     R.string.quest_buildingType_residential
    COMMERCIAL ->      R.string.quest_buildingType_commercial
    CIVIC ->           R.string.quest_buildingType_civic
    RELIGIOUS ->       R.string.quest_buildingType_religious
    GUARDHOUSE ->      R.string.quest_buildingType_guardhouse
    DIGESTER ->        R.string.quest_buildingType_digester
    SPORTS_HALL ->     R.string.quest_buildingType_sports_hall
    RIDING_HALL ->     R.string.quest_buildingType_riding_hall
    PRESBYTERY ->      R.string.quest_buildingType_presbytery
    BARN ->            R.string.quest_buildingType_barn
    COWSHED ->         R.string.quest_buildingType_cowshed
    STABLE ->          R.string.quest_buildingType_stable
    STY ->             R.string.quest_buildingType_sty
    TRANSFORMER_TOWER -> R.string.quest_buildingType_transformer_tower
    TENT ->            R.string.quest_buildingType_tent
    ELEVATOR ->        R.string.quest_buildingType_elevator
    CONSTRUCTION ->    null
}

private val BuildingType.descriptionResId: Int? get() = when (this) {
    HOUSE ->           R.string.quest_buildingType_house_description2
    APARTMENTS ->      R.string.quest_buildingType_apartments_description
    DETACHED ->        R.string.quest_buildingType_detached_description
    SEMI_DETACHED ->   R.string.quest_buildingType_semi_detached_description2
    TERRACE ->         R.string.quest_buildingType_terrace_description
    BUNGALOW ->        R.string.quest_buildingType_bungalow_description2
    HUT ->             R.string.quest_buildingType_hut_description
    INDUSTRIAL ->      R.string.quest_buildingType_industrial_description
    RETAIL ->          R.string.quest_buildingType_retail_description
    CARPORT ->         R.string.quest_buildingType_carport_description
    GARAGES ->         R.string.quest_buildingType_garages_description
    FARM ->            R.string.quest_buildingType_farmhouse_description
    FARM_AUXILIARY ->  R.string.quest_buildingType_farm_auxiliary_description
    SERVICE ->         R.string.quest_buildingType_service_description
    HANGAR ->          R.string.quest_buildingType_hangar_description
    HISTORIC ->        R.string.quest_buildingType_historic_description
    ABANDONED ->       R.string.quest_buildingType_abandoned_description
    RUINS ->           R.string.quest_buildingType_ruins_description
    RESIDENTIAL ->     R.string.quest_buildingType_residential_description
    COMMERCIAL ->      R.string.quest_buildingType_commercial_generic_description
    CIVIC ->           R.string.quest_buildingType_civic_description
    else ->            null
}

private val BuildingType.iconResId: Int? get() = when (this) {
    HOUSE ->           R.drawable.ic_building_house
    APARTMENTS ->      R.drawable.ic_building_apartments
    DETACHED ->        R.drawable.ic_building_detached
    SEMI_DETACHED ->   R.drawable.ic_building_semi_detached
    TERRACE ->         R.drawable.ic_building_terrace
    HOTEL ->           R.drawable.ic_building_hotel
    DORMITORY ->       R.drawable.ic_building_dormitory
    HOUSEBOAT ->       R.drawable.ic_building_houseboat
    BUNGALOW ->        R.drawable.ic_building_bungalow
    STATIC_CARAVAN ->  R.drawable.ic_building_static_caravan
    HUT ->             R.drawable.ic_building_hut
    INDUSTRIAL ->      R.drawable.ic_building_industrial
    RETAIL ->          R.drawable.ic_building_retail
    OFFICE ->          R.drawable.ic_building_office
    WAREHOUSE ->       R.drawable.ic_building_warehouse
    KIOSK ->           R.drawable.ic_building_kiosk
    STORAGE_TANK ->    R.drawable.ic_building_storage_tank
    KINDERGARTEN ->    R.drawable.ic_building_kindergarten
    SCHOOL ->          R.drawable.ic_building_school
    COLLEGE ->         R.drawable.ic_building_college
    SPORTS_CENTRE ->   R.drawable.ic_sport_volleyball
    HOSPITAL ->        R.drawable.ic_building_hospital
    STADIUM ->         R.drawable.ic_sport_volleyball
    GRANDSTAND ->      R.drawable.ic_sport_volleyball
    TRAIN_STATION ->   R.drawable.ic_building_train_station
    TRANSPORTATION ->  R.drawable.ic_building_transportation
    TRANSIT_SHELTER -> R.drawable.ic_building_transportation
    FIRE_STATION ->    R.drawable.ic_building_fire_truck
    UNIVERSITY ->      R.drawable.ic_building_university
    GOVERNMENT ->      R.drawable.ic_building_historic
    CHURCH ->          R.drawable.ic_religion_christian
    CHAPEL ->          R.drawable.ic_religion_christian
    CATHEDRAL ->       R.drawable.ic_religion_christian
    MOSQUE ->          R.drawable.ic_religion_muslim
    TEMPLE ->          R.drawable.ic_building_temple
    PAGODA ->          R.drawable.ic_building_temple
    SYNAGOGUE ->       R.drawable.ic_religion_jewish
    SHRINE ->          R.drawable.ic_building_temple
    CARPORT ->         R.drawable.ic_building_carport
    GARAGE ->          R.drawable.ic_building_garage
    GARAGES ->         R.drawable.ic_building_garages
    PARKING ->         R.drawable.ic_building_parking
    FARM ->            R.drawable.ic_building_farm_house
    FARM_AUXILIARY ->  R.drawable.ic_building_barn
    SILO ->            R.drawable.ic_building_silo
    GREENHOUSE ->      R.drawable.ic_building_greenhouse
    SHED ->            R.drawable.ic_building_shed
    ALLOTMENT_HOUSE -> R.drawable.ic_building_allotment_house
    ROOF ->            R.drawable.ic_building_roof
    BRIDGE ->          R.drawable.ic_building_bridge
    TOILETS ->         R.drawable.ic_building_toilets
    SERVICE ->         R.drawable.ic_building_service
    HANGAR ->          R.drawable.ic_building_hangar
    BUNKER ->          R.drawable.ic_building_bunker
    BOATHOUSE ->       R.drawable.ic_building_boathouse
    HISTORIC ->        R.drawable.ic_building_historic
    ABANDONED ->       R.drawable.ic_building_abandoned
    RUINS ->           R.drawable.ic_building_ruins
    RESIDENTIAL ->     R.drawable.ic_building_apartments
    COMMERCIAL ->      R.drawable.ic_building_office
    CIVIC ->           R.drawable.ic_building_civic
    RELIGIOUS ->       R.drawable.ic_building_temple
    GUARDHOUSE ->      R.drawable.ic_building_guardhouse
    DIGESTER ->        R.drawable.ic_building_storage_tank
    SPORTS_HALL ->     R.drawable.ic_sport_volleyball
    RIDING_HALL ->     R.drawable.ic_sport_equestrian
    PRESBYTERY ->      R.drawable.ic_religion_christian
    BARN ->            R.drawable.ic_building_barn
    COWSHED ->         R.drawable.ic_building_barn
    STABLE ->          R.drawable.ic_building_barn
    STY ->             R.drawable.ic_building_barn
    TRANSFORMER_TOWER -> R.drawable.ic_building_service
    TENT ->            R.drawable.ic_building_greenhouse
    ELEVATOR ->        R.drawable.ic_building_bridge
    CONSTRUCTION ->    null
}
