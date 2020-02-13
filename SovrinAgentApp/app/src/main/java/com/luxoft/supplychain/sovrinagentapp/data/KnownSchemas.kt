package com.luxoft.supplychain.sovrinagentapp.data

/**
 * Information about Indy schemas that needs to be known for the app to function.
 * May need updating after creation of new schemas.
 * */
object KnownSchemas {
    object PersonalId {
        val schemaName = "Swiss Travel Pass"

        object attributes {
            val firstName = "First_Name"
            val birthDate = "Birth_Date"
            val photo = "Photo"
            val secondName = "Second_Name"
            val swissPassNum = "Swiss_Pass_Num"
        }
//        val schemaName = "Patient Demographics"
//
//        object attributes {
//            val name = "Full_Legal_Name"
//        }
    }

    object Common {
        object attributes {
            val type = "Credential_Type"
            val name = "Credential_Name"
            val issuer = "Credential_Issuer"
        }
    }
}