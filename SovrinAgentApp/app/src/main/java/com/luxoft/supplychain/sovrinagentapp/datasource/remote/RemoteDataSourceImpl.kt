package com.luxoft.supplychain.sovrinagentapp.datasource.remote

import android.graphics.Bitmap
import com.luxoft.blockchainlab.corda.hyperledger.indy.AgentConnection
import com.luxoft.blockchainlab.hyperledger.indy.models.ProofInfo
import com.luxoft.blockchainlab.hyperledger.indy.models.ProofRequest
import com.luxoft.supplychain.sovrinagentapp.data.ApplicationState
import com.luxoft.supplychain.sovrinagentapp.data.Invite
import com.luxoft.supplychain.sovrinagentapp.data.SharedPreferencesStore
import com.luxoft.supplychain.sovrinagentapp.data.idatasource.RemoteDataSource
import io.reactivex.Single
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.luxoft.blockchainlab.corda.hyperledger.indy.IndyPartyConnection
import com.luxoft.blockchainlab.hyperledger.indy.utils.*
import com.luxoft.supplychain.sovrinagentapp.application.*
import java.time.LocalDateTime


class RemoteDataSourceImpl constructor(private val agentConnection: AgentConnection, private val applicationState: ApplicationState, private val sharedPreferencesStore: SharedPreferencesStore)
    : RemoteDataSource {

    override fun getCredentials(url: String): Single<String> {
        return Single.create<String> { s ->
            run {
                try {
                    agentConnection.acceptInvite(SerializationUtils.jSONToAny<Invite>(url).invite).toBlocking().value().apply {
                        //                        s.onNext("Receiving digital credential")
                        do {
                            val credOffer = try {
                                receiveCredentialOffer().timeout(5, TimeUnit.SECONDS).toBlocking().value()
                            } catch (e: RuntimeException) {
                                //End of waiting for new credentials
                                if (e.cause !is TimeoutException)
                                    throw e
                                null
                            }?.apply {
                                val indyUser = applicationState.indyState.indyUser.value!!
                                val credentialRequest = indyUser.createCredentialRequest(indyUser.walletUser.getIdentityDetails().did, this)
                                sendCredentialRequest(credentialRequest)
                                val credential = receiveCredential().toBlocking().value()
//                                s.onNext("Verifying digital credential")
                                indyUser.checkLedgerAndReceiveCredential(credential, credentialRequest, this)
                            }
                        } while (credOffer != null)
                        applicationState.updateWalletCredentials()
                        s.onSuccess("completed")
                    }
                } catch (er: Exception) {
                    s.onError(er)
                }
            }
        }
    }

    override fun sendProofOnRequest(url: String): Single<String> {
        return Single.create<String> { s ->
            run {
                try {
                    agentConnection.acceptInvite(SerializationUtils.jSONToAny<Invite>(url).invite).toBlocking().value().apply {
                        val proofRequest = receiveProofRequest().toBlocking().value()
                        val requestedData: Set<String> = proofRequest.requestedAttributes.keys + proofRequest.requestedPredicates.keys
                        val requestedDataStr = requestedData.joinToString(separator = ", ")
                        sharedPreferencesStore.writeString(sharedPreferencesRequstedDataName, sharedPreferencesRequstedDataKey, requestedDataStr)
                        val proofFromLedgerData: ProofInfo = applicationState.indyState.indyUser.value!!.createProofFromLedgerData(proofRequest)
                        sendProof(proofFromLedgerData)
                        s.onSuccess("completed")
                    }
                } catch (er: Exception) {
                    s.onError(er)
                }
            }
        }
    }

    //use for buyer wallet app role
    override fun receiveProofRequest(url: String): Single<ProofRequest> {
        return Single.create<ProofRequest> { s ->
            run {
                try {
                    agentConnection.acceptInvite(SerializationUtils.jSONToAny<Invite>(url).invite).toBlocking().value().apply {
                        val proofRequest = receiveProofRequest().toBlocking().value()
                        sharedPreferencesStore.writeString(sharedPreferencesLastConnectionDiDName, sharedPreferencesLastConnectionDiDKey, this.partyDID())
                        s.onSuccess(proofRequest)
                    }
                } catch (er: Exception) {
                    s.onError(er)
                }
            }
        }
    }

    //use for buyer wallet app role
    override fun sendProof(proofRequest: ProofRequest): Single<String> {
        return Single.create<String> { s ->
            run {
                try {
                    val requestedData: Set<String> = proofRequest.requestedAttributes.keys + proofRequest.requestedPredicates.keys
                    val requestedDataStr = requestedData.joinToString(separator = ", ")
                    sharedPreferencesStore.writeString(sharedPreferencesRequstedDataName, sharedPreferencesRequstedDataKey, requestedDataStr)
                    val did = sharedPreferencesStore.readString(sharedPreferencesLastConnectionDiDName, sharedPreferencesLastConnectionDiDKey)
                    val proofFromLedgerData: ProofInfo = applicationState.indyState.indyUser.value!!.createProofFromLedgerData(proofRequest)
                    agentConnection.getIndyPartyConnection(did!!).toBlocking().value().apply {
                        this!!.sendProof(proofFromLedgerData)
                        s.onSuccess("completed")
                    }
                } catch (er: Exception) {
                    s.onError(er)
                }
            }
        }
    }

    //use for kiosk verifier app role
    override fun getInviteQRCode(): Single<Bitmap> {
        return Single.create<Bitmap> { bitmap ->
            run {
                val inviteUrl: String = agentConnection.generateInvite().toBlocking().value();
                sharedPreferencesStore.writeString(sharedPreferencesLastInviteUrlName, sharedPreferencesLastInviteUrlKey, inviteUrl)
                val multiFormatWriter = MultiFormatWriter()
                try {
                    val bitMatrix = multiFormatWriter.encode(inviteUrl, BarcodeFormat.QR_CODE, 200, 200)
                    val barcodeEncoder = BarcodeEncoder()
                    bitmap.onSuccess(barcodeEncoder.createBitmap(bitMatrix))
                } catch (e: WriterException) {
                    e.printStackTrace()
                    bitmap.onError(e)
                }
            }
        }
    }

    //use for kiosk verifier app role
    override fun waitForInvitedParty(timeout: Long): Single<IndyPartyConnection> {
        return Single.create<IndyPartyConnection> { connection ->
            run {
                try {
                    connection.onSuccess(agentConnection.waitForInvitedParty(sharedPreferencesStore.readString(sharedPreferencesLastInviteUrlName, sharedPreferencesLastInviteUrlKey)!!, timeout).toBlocking().value())
                } catch (e: Exception) {
                    connection.onError(e)
                }
            }
        }
    }

    //use for kiosk verifier app role
    override fun sendProofRequestReceiveAndVerify(indyPartyConnection: IndyPartyConnection?): Single<Boolean> {
        return Single.create<Boolean> { boolean ->
            run {
                try {
                    val proofRequest: ProofRequest = createProofRequest()
                    indyPartyConnection!!.sendProofRequest(proofRequest).apply {
                        indyPartyConnection.receiveProof().toBlocking().value().apply {
                            val verified: Boolean = applicationState.indyState.indyUser.value!!.verifyProofWithLedgerData(proofRequest, this)
                            boolean.onSuccess(verified)
                        }
                    }
                } catch (e: Exception) {
                    boolean.onError(e)
                }
            }
        }

    }

    private fun createProofRequest(): ProofRequest {
        val proofReq = proofRequest("proof_req", "0.1") {
//            reveal("firstName")
//            reveal("birthDate")
//            reveal("photo")
//            reveal("secondName")
//            reveal("swissPassNum")
            //greater than 16 years in sec
            proveGreaterThan("Birth_Date", 504924600)
//            reveal("medicalid") { FilterProperty.IssuerDid shouldBe "H4KaAh8W8DUaj47s4PXQEB" }
        }
        return proofReq;
    }

}