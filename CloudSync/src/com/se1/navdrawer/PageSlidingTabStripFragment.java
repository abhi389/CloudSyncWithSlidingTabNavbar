package com.se1.navdrawer;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.astuetz.PagerSlidingTabStrip;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.se1.DropBox.DropboxDownload;
import com.se1.main.R;

public class PageSlidingTabStripFragment extends Fragment {
    public static DropboxAPI<AndroidAuthSession> getmApi() {
        return mApi;
    }

    public static void setmApi(DropboxAPI<AndroidAuthSession> mApi) {
        PageSlidingTabStripFragment.mApi = mApi;
    }

    private static DropboxAPI<AndroidAuthSession> mApi;
	public static final String TAG = PageSlidingTabStripFragment.class
			.getSimpleName();

	public static PageSlidingTabStripFragment newInstance() {
		return new PageSlidingTabStripFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setmApi(NavigationMainActivity.getmApi());
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.pager, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) view
				.findViewById(R.id.tabs);
		ViewPager pager = (ViewPager) view.findViewById(R.id.pager);
		MyPagerAdapter adapter = new MyPagerAdapter(getChildFragmentManager());
        pager.setAdapter(adapter);
		tabs.setViewPager(pager);

	}

	public class MyPagerAdapter extends FragmentPagerAdapter {
        private DropboxAPI<AndroidAuthSession> mApi;
		public MyPagerAdapter(android.support.v4.app.FragmentManager fm) {
			super(fm);
		}

		private final String[] TITLES = { "All Files", "Images",
				"Documents" };

		@Override
		public CharSequence getPageTitle(int position) {
			return TITLES[position];
		}

		@Override
		public int getCount() {
			return TITLES.length;
		}

		@Override
		public SherlockFragment getItem(int position) {
            mApi = PageSlidingTabStripFragment.getmApi();
           if( position == 0 && mApi != null)
           {
               //Log.d("inside if","if");
              // final Handler handler = new Handler();
               final DropboxDownload dropboxDownload= new DropboxDownload();
               dropboxDownload.setDataFromLogin(mApi);
               //tryFragment t=new tryFragment();
               return dropboxDownload;//DropboxDownload.setDataFromLogin(getActivity().getApplicationContext(),mApi);

           }
		 else return SuperAwesomeCardFragment.newInstance(position);
		}

	}


}
