package org.qtum.wallet.ui.fragment.send_fragment;

import org.qtum.wallet.R;
import org.qtum.wallet.model.Currency;
import org.qtum.wallet.model.CurrencyToken;
import org.qtum.wallet.model.contract.Token;
import org.qtum.wallet.model.gson.FeePerKb;
import org.qtum.wallet.model.gson.UnspentOutput;
import org.qtum.wallet.model.gson.call_smart_contract_response.CallSmartContractResponse;
import org.qtum.wallet.model.gson.token_balance.Balance;
import org.qtum.wallet.model.gson.token_balance.TokenBalance;
import org.qtum.wallet.ui.base.base_fragment.BaseFragment;
import org.qtum.wallet.ui.base.base_fragment.BaseFragmentPresenterImpl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class SendPresenterImpl extends BaseFragmentPresenterImpl implements SendPresenter {

    private SendView mSendFragmentView;
    private SendInteractor mSendBaseFragmentInteractor;
    private boolean mNetworkConnectedFlag = false;
    private List<Token> mTokenList;
    private double minFee;
    private double maxFee = 0.2;

    public SendPresenterImpl(SendView sendFragmentView, SendInteractor interactor) {
        mSendFragmentView = sendFragmentView;
        mSendBaseFragmentInteractor = interactor;
    }

    @Override
    public void initializeViews() {
        super.initializeViews();
        mTokenList = new ArrayList<>();
        for (Token token : getInteractor().getTokenList()) {
            if (token.isSubscribe()) {
                mTokenList.add(token);
            }
        }
        if (!mTokenList.isEmpty()) {
            getView().setUpCurrencyField(R.string.default_currency);
        } else {
            getView().hideCurrencyField();
        }
        getInteractor().getFeePerKbObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<FeePerKb>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(FeePerKb feePerKb) {
                        getInteractor().handleFeePerKbValue(feePerKb);

                        minFee = feePerKb.getFeePerKb().doubleValue();
                        getView().updateFee(minFee, maxFee);
                    }
                });
    }

    @Override
    public void handleBalanceChanges(final BigDecimal unconfirmedBalance, final BigDecimal balance) {
        Observable.defer(new Func0<Observable<String>>() {
            @Override
            public Observable<String> call() {
                String balanceString = balance.toString();
                if (balanceString != null) {
                    return Observable.just(balanceString);
                } else {
                    return Observable.error(new Throwable("Balance is null"));
                }
            }
        })
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String balanceString) {
                        getView().handleBalanceUpdating(balanceString, unconfirmedBalance);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });

    }

    @Override
    public void searchAndSetUpCurrency(String currency) {
        for (Token token : getInteractor().getTokenList()) {
            if (token.getContractAddress().equals(currency)) {
                getView().setUpCurrencyField(new CurrencyToken(token.getContractName(), token));
                return;
            }
        }
    }

    @Override
    public void onCurrencyChoose(Currency currency) {
        getView().setUpCurrencyField(currency);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getView().removePermissionResultListener();
        //TODO:unsubscribe rx
    }

    @Override
    public SendView getView() {
        return mSendFragmentView;
    }

    private SendInteractor getInteractor() {
        return mSendBaseFragmentInteractor;
    }

    @Override
    public void onResponse(String publicAddress, double amount, String tokenAddress) {
        String tokenName = validateTokenExistance(tokenAddress);
        getView().updateData(publicAddress, amount, tokenName);
    }

    private String validateTokenExistance(String tokenAddress) {
        if (getView().isTokenEmpty(tokenAddress)) {
            return "";
        }

        String contractName = getInteractor().validateTokenExistance(tokenAddress);
        if (contractName != null) {
            return contractName;
        }

        getView().setAlertDialog(org.qtum.wallet.R.string.token_not_found, "Ok", BaseFragment.PopUpType.error);
        return "";
    }

    @Override
    public void onResponseError() {
        getView().errorRecognition();
    }

    private String availableAddress = null;
    public String params = null;

    @Override
    public void send() {
        if (mNetworkConnectedFlag) {
            final double feeDouble = Double.valueOf(getView().getFeeInput());
            if (feeDouble < minFee || feeDouble > maxFee) {
                getView().dismissProgressDialog();
                getView().setAlertDialog(org.qtum.wallet.R.string.error, R.string.invalid_fee, "Ok", BaseFragment.PopUpType.error);
                return;
            }

            if (!getView().isValidAmount()) {
                return;
            }

            getView().showPinDialog();

        } else {
            getView().setAlertDialog(org.qtum.wallet.R.string.no_internet_connection, org.qtum.wallet.R.string.please_check_your_network_settings, "Ok", BaseFragment.PopUpType.error);
        }
    }

    @Override
    public void onPinSuccess() {
        Currency currency = getView().getCurrency();
        String from = getView().getFromAddress();
        String address = getView().getAddressInput();
        String amount = getView().getAmountInput();
        final double feeDouble = Double.valueOf(getView().getFeeInput());
        final String fee = validateFee(feeDouble);

        if (currency.getName().equals("Qtum " + getView().getStringValue(org.qtum.wallet.R.string.default_currency))) {
            getInteractor().sendTx(from, address, amount, fee, getView().getSendTransactionCallback());
        } else {
            for (final Token token : mTokenList) {
                String contractAddress = token.getContractAddress();
                if (contractAddress.equals(((CurrencyToken) currency).getToken().getContractAddress())) {

                    String resultAmount = amount;

                    if (token.getDecimalUnits() != null) {
                        resultAmount = String.valueOf((int) (Double.valueOf(amount) * Math.pow(10, token.getDecimalUnits())));
                        resultAmount = String.valueOf(Integer.valueOf(resultAmount));
                    }

                    TokenBalance tokenBalance = getView().getTokenBalance(contractAddress);

                    availableAddress = null;

                    if (!from.equals("")) {
                        for (Balance balance : tokenBalance.getBalances()) {
                            if (balance.getAddress().equals(from)) {
                                if (balance.getBalance().floatValue() >= Float.valueOf(resultAmount)) {
                                    availableAddress = balance.getAddress();
                                    break;
                                } else {
                                    break;
                                }
                            }
                        }
                    } else {
                        for (Balance balance : tokenBalance.getBalances()) {
                            if (balance.getBalance().floatValue() >= Float.valueOf(resultAmount)) {
                                availableAddress = balance.getAddress();
                                break;
                            }
                        }
                    }

                    if (!getView().isValidAvailableAddress(availableAddress)) {
                        return;
                    }

                    createAbiMethodParams(address, resultAmount, token, fee);

                    break;
                }
            }
        }

    }

    private void createAbiMethodParams(String address, String resultAmount, final Token token, final String fee) {
        getInteractor().createAbiMethodParamsObservable(address, resultAmount, "transfer")
                .flatMap(new Func1<String, Observable<CallSmartContractResponse>>() {
                    @Override
                    public Observable<CallSmartContractResponse> call(String s) {
                        params = s;
                        return getInteractor().callSmartContractObservable(token, s);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<CallSmartContractResponse>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        getView().dismissProgressDialog();
                        getView().setAlertDialog(org.qtum.wallet.R.string.error, e.getMessage(), "Ok", BaseFragment.PopUpType.error);
                    }

                    @Override
                    public void onNext(CallSmartContractResponse response) {
                        if (!response.getItems().get(0).getExcepted().equals("None")) {
                            getView().setAlertDialog(org.qtum.wallet.R.string.error,
                                    response.getItems().get(0).getExcepted(), "Ok", BaseFragment.PopUpType.error);
                            return;
                        }
                        createTx(params, token.getContractAddress(), availableAddress, /*TODO callSmartContractResponse.getItems().get(0).getGasUsed()*/ 2000000, fee);
                    }
                });

    }

    private String validateFee(Double fee) {
        return getInteractor().getValidatedFee(fee);
    }

    private void createTx(final String abiParams, final String contractAddress, String senderAddress, final int gasLimit, final String fee) {
        getInteractor().getUnspentOutputs(senderAddress, new SendInteractorImpl.GetUnspentListCallBack() {
            @Override
            public void onSuccess(List<UnspentOutput> unspentOutputs) {
                String txHex = getInteractor().createTransactionHash(abiParams, contractAddress, unspentOutputs, gasLimit, fee);
                getInteractor().sendTx(txHex, getView().getSendTransactionCallback());
            }

            @Override
            public void onError(String error) {
                getView().dismissProgressDialog();
                getView().setAlertDialog(org.qtum.wallet.R.string.error, error, "Ok", BaseFragment.PopUpType.error);
            }
        });
    }

    @Override
    public void updateNetworkSate(boolean networkConnectedFlag) {
        mNetworkConnectedFlag = networkConnectedFlag;
    }

    /**
     * Getter for unit tests
     */
    public double getMinFee() {
        return minFee;
    }

    /**
     * Getter for unit tests
     */
    public List<Token> getTokenList() {
        return mTokenList;
    }

    /**
     * Getter for unit tests
     */
    public String getAvailableAddress() {
        return availableAddress;
    }

    /**
     * Setter for unit tests
     */
    public void setTokenList(List<Token> tokenList) {
        this.mTokenList = tokenList;
    }

    /**
     * Setter for unit tests
     */
    public void setMinFee(double minFee) {
        this.minFee = minFee;
    }

    /**
     * Setter for unit tests
     */
    public void setMaxFee(double maxFee) {
        this.maxFee = maxFee;
    }
}