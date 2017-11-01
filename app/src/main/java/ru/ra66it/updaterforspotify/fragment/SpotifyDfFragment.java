package ru.ra66it.updaterforspotify.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.ra66it.updaterforspotify.QueryPreferneces;
import ru.ra66it.updaterforspotify.R;
import ru.ra66it.updaterforspotify.api.SpotifyDogfoodApi;
import ru.ra66it.updaterforspotify.model.Spotify;
import ru.ra66it.updaterforspotify.utils.UtilsDownloadSpotify;
import ru.ra66it.updaterforspotify.utils.UtilsFAB;
import ru.ra66it.updaterforspotify.utils.UtilsNetwork;
import ru.ra66it.updaterforspotify.utils.UtilsSpotify;
import ru.ra66it.updaterforspotify.notification.VisibleFragment;

import static android.view.View.GONE;

/**
 * Created by 2Rabbit on 28.09.2017.
 */

public class SpotifyDfFragment extends VisibleFragment {

    private static final String TAG = SpotifyDfFragment.class.getSimpleName();

    private CardView cvLatestDf;
    private TextView lblLatestVersion;
    private TextView lblInstallVersion;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeToRefresh;
    private FloatingActionButton fabDownloadButton;

    private LinearLayout layoutCards;

    private String latestLink;
    private String latestVersionName = "";
    private String latestVersionNumber = "";
    private String installVersion;

    private boolean hasError = false;


    public static SpotifyDfFragment newInstance() {
        return new SpotifyDfFragment();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.spotify_df_fragment, container, false);


        swipeToRefresh = (SwipeRefreshLayout) v.findViewById(R.id.swipeContainer);
        layoutCards = (LinearLayout) v.findViewById(R.id.layout_cards);
        cvLatestDf = (CardView) v.findViewById(R.id.cv_latest_df);
        lblLatestVersion = (TextView) v.findViewById(R.id.lbl_latest_version);
        lblInstallVersion = (TextView) v.findViewById(R.id.lbl_install_version);
        progressBar = (ProgressBar) v.findViewById(R.id.latest_progress_bar);
        fabDownloadButton = (FloatingActionButton) v.findViewById(R.id.fab);

        swipeToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchData();
                swipeToRefresh.setRefreshing(false);
            }
        });

        fabDownloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UtilsDownloadSpotify.downloadSpotify(getContext(), latestLink, latestVersionName);
            }
        });
        fetchData();

        if (QueryPreferneces.isFirstLaunch(getActivity())) {
            FragmentManager manager = getFragmentManager();
            ChooseNotificationDialog dialog = new ChooseNotificationDialog();
            dialog.setCancelable(false);
            dialog.show(manager, "choose");

        }

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkInstalledSpotifyVersion();

    }



    private void checkInstalledSpotifyVersion() {
        if (UtilsSpotify.isSpotifyInstalled(getActivity())) {
            installVersion = UtilsSpotify.getInstalledSpotifyVersion(getActivity());
            lblInstallVersion.setText(installVersion);
            if (UtilsSpotify.isDogFoodInstalled(getActivity())) {
                fabDownloadButton.setImageResource(R.drawable.ic_autorenew_black_24dp);
            }
            fillData();
        } else {
            UtilsFAB.hideOrShowFAB(fabDownloadButton, false);
            fabDownloadButton.setImageResource(R.drawable.ic_file_download_black_24dp);
            lblInstallVersion.setText(getString(R.string.dogfood_not_installed));
            if (hasError) {
                UtilsFAB.hideOrShowFAB(fabDownloadButton, true);
            }
        }

    }

    private void fillData() {
        if (!latestVersionNumber.equals("0.0.0.0")) {
            lblLatestVersion.setText(latestVersionNumber);
            if (UtilsSpotify.isSpotifyInstalled(getActivity()) &&
                    UtilsSpotify.isDogfoodUpdateAvailable(installVersion, latestVersionNumber)) {
                UtilsFAB.hideOrShowFAB(fabDownloadButton, false);
               // Install new version
                cvLatestDf.setVisibility(View.VISIBLE);

            } else if (!UtilsSpotify.isSpotifyInstalled(getActivity())) {
                UtilsFAB.hideOrShowFAB(fabDownloadButton, false);
              // Install spotify now
                cvLatestDf.setVisibility(View.VISIBLE);

            } else if (!UtilsSpotify.isDogFoodInstalled(getActivity())){
                UtilsFAB.hideOrShowFAB(fabDownloadButton, false);
                cvLatestDf.setVisibility(View.VISIBLE);

            } else {
                //have latest version
                UtilsFAB.hideOrShowFAB(fabDownloadButton, true);
                cvLatestDf.setVisibility(View.GONE);
                lblInstallVersion.setText(getString(R.string.up_to_date));
            }

        }
    }


    private void fetchData() {
        if (UtilsNetwork.isNetworkAvailable(getActivity())) {
            errorLayout(false);

            latestVersionNumber = "0.0.0.0";
            lblLatestVersion.setVisibility(GONE);
            progressBar.setVisibility(View.VISIBLE);

            SpotifyDogfoodApi.Factory.getInstance().getLatestDogFood().enqueue(new Callback<Spotify>() {
                @Override
                public void onResponse(Call<Spotify> call, Response<Spotify> response) {

                    try {
                        latestLink = response.body().getBody();
                        latestVersionNumber = response.body().getTagName();
                        latestVersionName = response.body().getName();
                        fillData();

                    } catch (NullPointerException e) {
                        errorLayout(true);
                        Snackbar.make(swipeToRefresh, getString(R.string.error),
                                Snackbar.LENGTH_SHORT).show();
                    }


                    progressBar.setVisibility(View.GONE);
                    lblLatestVersion.setVisibility(View.VISIBLE);
                }

                @Override
                public void onFailure(Call<Spotify> call, Throwable t) {
                    errorLayout(true);
                    Snackbar.make(swipeToRefresh, getString(R.string.error),
                            Snackbar.LENGTH_SHORT).show();
                    UtilsFAB.hideOrShowFAB(fabDownloadButton, true);
                }
            });

        } else {
            errorLayout(true);
            Snackbar.make(swipeToRefresh, getString(R.string.no_internet_connection),
                    Snackbar.LENGTH_SHORT).show();
        }

    }


    private void errorLayout(boolean bool) {
        if (bool) {
            hasError = true;
            layoutCards.setVisibility(View.GONE);
            UtilsFAB.hideOrShowFAB(fabDownloadButton, true);
        } else {
            hasError = false;
            layoutCards.setVisibility(View.VISIBLE);
            UtilsFAB.hideOrShowFAB(fabDownloadButton, true);
        }

    }

}
