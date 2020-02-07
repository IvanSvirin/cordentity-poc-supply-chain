package com.luxoft.supplychain.sovrinagentapp.domain.irepository

import com.luxoft.blockchainlab.hyperledger.indy.models.ProofRequest
import io.reactivex.Observable
import io.reactivex.Single


interface IndyRepository {
    fun getCredentials(url : String) : Single<String>
    fun sendProofOnRequest(url: String): Single<String>
    fun receiveProofRequest(url: String): Single<ProofRequest>
    fun sendProof(proofRequest: ProofRequest): Single<String>

}