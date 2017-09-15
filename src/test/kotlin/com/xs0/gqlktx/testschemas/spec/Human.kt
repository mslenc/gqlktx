package com.xs0.gqlktx.testschemas.spec

interface Human : Sentient, HumanOrAlien, DogOrHuman {
    val pets: List<Pet>

    val previousPets: Array<Pet>

    val siblings: List<Array<Human>>

    val boss: HumanOrAlien
}
