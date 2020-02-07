package com.luxoft.supplychain.sovrinagentapp.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.luxoft.blockchainlab.hyperledger.indy.models.ProofRequest
import com.luxoft.supplychain.sovrinagentapp.domain.usecase.GetCredentialsUseCase
import com.luxoft.supplychain.sovrinagentapp.domain.usecase.GetProofRequestUseCase
import com.luxoft.supplychain.sovrinagentapp.domain.usecase.SendProofUseCase
import com.luxoft.supplychain.sovrinagentapp.utils.Resource
import com.luxoft.supplychain.sovrinagentapp.utils.setError
import com.luxoft.supplychain.sovrinagentapp.utils.setLoading
import com.luxoft.supplychain.sovrinagentapp.utils.setSuccess
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class IndyViewModel constructor(private val getCredentialsUseCase: GetCredentialsUseCase, private val getProofRequestUseCase: GetProofRequestUseCase,
                                private val sendProofUseCase: SendProofUseCase) :
        ViewModel() {

    val responseCredentialsGot = MutableLiveData<Resource<String>>()
    val proofRequest = MutableLiveData<Resource<ProofRequest>>()
    val responseProfSent = MutableLiveData<Resource<String>>()

    private val compositeDisposable = CompositeDisposable()

    fun getCredentials(url: String) =
            compositeDisposable.add(getCredentialsUseCase.get(url)
                    .doOnSubscribe { responseCredentialsGot.setLoading() }
                    .subscribeOn(Schedulers.io())
                    .subscribe({ responseCredentialsGot.setSuccess(it) }, { responseCredentialsGot.setError(it.message) })
            )

    fun receiveProofRequest(url: String) =
            compositeDisposable.add(getProofRequestUseCase.get(url)
                    .doOnSubscribe { proofRequest.setLoading() }
                    .subscribeOn(Schedulers.io())
                    .subscribe({ proofRequest.setSuccess(it) }, { proofRequest.setError(it.message) })
            )

    fun sendProof(proofRequest: ProofRequest) =
            compositeDisposable.add(sendProofUseCase.get(proofRequest)
                    .doOnSubscribe { responseProfSent.setLoading() }
                    .subscribeOn(Schedulers.io())
                    .subscribe({ responseProfSent.setSuccess(it) }, { responseProfSent.setError(it.message) })
            )

    override fun onCleared() {
        compositeDisposable.dispose()
        super.onCleared()
    }
}