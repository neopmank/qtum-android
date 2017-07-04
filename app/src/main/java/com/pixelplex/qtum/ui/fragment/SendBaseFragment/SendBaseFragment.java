package com.pixelplex.qtum.ui.fragment.SendBaseFragment;

import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pixelplex.qtum.R;
import com.pixelplex.qtum.ui.activity.main_activity.MainActivity;
import com.pixelplex.qtum.ui.fragment.BaseFragment.BaseFragment;
import com.pixelplex.qtum.utils.FontManager;

import butterknife.BindView;
import butterknife.OnClick;


public class SendBaseFragment extends BaseFragment implements SendBaseFragmentView {

    private static final String IS_QR_CODE_RECOGNITION = "is_qr_code_recognition";
    private static final String ADDRESS = "address";
    private static final String AMOUNT = "amount";

    @BindView(R.id.et_receivers_address)
    TextInputEditText mTextInputEditTextAddress;
    @BindView(R.id.et_amount)
    TextInputEditText mTextInputEditTextAmount;

    @BindView(R.id.til_receivers_address)
    TextInputLayout tilAdress;
    @BindView(R.id.til_amount)
    TextInputLayout tilAmount;

    @BindView(R.id.bt_send)
    Button mButtonSend;
    @BindView(R.id.ibt_back)
    ImageButton mImageButtonBack;
    @BindView(R.id.tv_toolbar_send)
    TextView mTextViewToolBar;
    @BindView(R.id.progress_bar)
    ProgressBar mProgressBar;
    @BindView(R.id.tv_total_balance_number)
    TextView mTextViewTotalBalanceNumber;
    @BindView(R.id.rl_send)
    RelativeLayout mRelativeLayoutBase;
    @BindView(R.id.ll_currency)
    LinearLayout mLinearLayoutCurrency;
    @BindView(R.id.tv_currency)
    TextView mTextViewCurrency;

    private SendBaseFragmentPresenterImpl sendBaseFragmentPresenter;

    @BindView(R.id.bt_qr_code)
    ImageButton mButtonQrCode;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @OnClick({R.id.bt_qr_code, R.id.bt_send, R.id.ibt_back, R.id.ll_currency})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_qr_code:
                getPresenter().onClickQrCode();
                break;
            case R.id.bt_send:
                String[] sendInfo = new String[3];
                sendInfo[0] = mTextInputEditTextAddress.getText().toString();
                sendInfo[1] = mTextInputEditTextAmount.getText().toString();
                if(mLinearLayoutCurrency.getVisibility()==View.VISIBLE){
                    sendInfo[2] = mTextViewCurrency.getText().toString();
                } else {
                    sendInfo[2] = "Qtum "+getString(R.string.default_currency);
                }
                getPresenter().send(sendInfo);
                break;
            case R.id.ibt_back:
                getActivity().onBackPressed();
                break;
            case R.id.ll_currency:
                getPresenter().onCurrencyClick();
                break;
        }
    }

    public static SendBaseFragment newInstance(boolean qrCodeRecognition, String address, String amount) {
        SendBaseFragment sendBaseFragment = new SendBaseFragment();
        Bundle args = new Bundle();
        args.putBoolean(IS_QR_CODE_RECOGNITION, qrCodeRecognition);
        args.putString(ADDRESS,address);
        args.putString(AMOUNT,amount);
        sendBaseFragment.setArguments(args);
        return sendBaseFragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        getPresenter().isQrCodeRecognition(getArguments().getBoolean(IS_QR_CODE_RECOGNITION));
        getArguments().putBoolean(IS_QR_CODE_RECOGNITION, false);
    }

    @Override
    protected void createPresenter() {
        sendBaseFragmentPresenter = new SendBaseFragmentPresenterImpl(this);
    }

    @Override
    protected SendBaseFragmentPresenterImpl getPresenter() {
        return sendBaseFragmentPresenter;
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_send_base;
    }

    @Override
    public void openInnerFragmentForResult(Fragment fragment) {
        int code_response = 200;
        fragment.setTargetFragment(this, code_response);
        getFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                .add(R.id.fragment_container_send_base, fragment, fragment.getClass().getCanonicalName())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void qrCodeRecognitionToolBar() {
        mButtonQrCode.setVisibility(View.GONE);
        mTextViewToolBar.setText(R.string.qr_code);
        mImageButtonBack.setVisibility(View.VISIBLE);
    }

    @Override
    public void sendToolBar() {
        if (mButtonQrCode != null) {
            mTextViewToolBar.setText(R.string.send);
            mButtonQrCode.setVisibility(View.VISIBLE);
            mImageButtonBack.setVisibility(View.GONE);
        }
    }

    @Override
    public void initializeViews() {
        super.initializeViews();
        showBottomNavView(true);
        ((MainActivity) getActivity()).setIconChecked(3);
        mImageButtonBack.setVisibility(View.GONE);
        mRelativeLayoutBase.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b) {
                    hideKeyBoard();
                }
            }
        });
        String address = getArguments().getString(ADDRESS, "");
        String amount = getArguments().getString(AMOUNT, "");
        mTextInputEditTextAmount.setText(amount);
        mTextInputEditTextAddress.setText(address);


        mTextInputEditTextAddress.setTypeface(FontManager.getInstance().getFont(getString(R.string.simplonMonoRegular)));
        mTextInputEditTextAmount.setTypeface(FontManager.getInstance().getFont(getString(R.string.simplonMonoRegular)));

        tilAdress.setTypeface(FontManager.getInstance().getFont(getString(R.string.simplonMonoRegular)));
        tilAmount.setTypeface(FontManager.getInstance().getFont(getString(R.string.simplonMonoRegular)));
    }

    @Override
    public void updateData(String publicAddress, double amount) {
        mTextInputEditTextAddress.setText(publicAddress);
        mTextInputEditTextAmount.setText(String.valueOf(amount));
    }

    @Override
    public void errorRecognition() {

    }

    @Override
    public void setProgressBar() {
        mProgressBar.setVisibility(View.VISIBLE);
        mTextViewTotalBalanceNumber.setVisibility(View.GONE);
    }

    @Override
    public void updateAvailableBalance(String balance) {
        mProgressBar.setVisibility(View.GONE);
        mTextViewTotalBalanceNumber.setVisibility(View.VISIBLE);
        mTextViewTotalBalanceNumber.setText(balance);
    }

    @Override
    public void setUpCurrencyField(String currency) {
        mLinearLayoutCurrency.setVisibility(View.VISIBLE);
        mTextViewCurrency.setText(currency);
    }

    @Override
    public Fragment getFragment() {
        return this;
    }

    @Override
    public void hideCurrencyField() {
        mLinearLayoutCurrency.setVisibility(View.GONE);
    }

    @Override
    public void setAdressAndAmount(final String address, final String anount) {
        mTextInputEditTextAddress.post(new Runnable() {
            @Override
            public void run() {
                mTextInputEditTextAddress.setText(address);
                mTextInputEditTextAmount.setText(anount);
            }
        });
    }


    public void onResponse(String pubAddress, Double amount) {
        getPresenter().onResponse(pubAddress, amount);
    }

    public void onCurrencyChoose(String currency){
        getPresenter().onCurrencyChoose(currency);
    }

    public void onResponseError() {
        setAlertDialog("Invalid QR Code","OK", PopUpType.error);
    }

    @Override
    public void setSoftMode() {
        super.setSoftMode();
        getMainActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    }
}