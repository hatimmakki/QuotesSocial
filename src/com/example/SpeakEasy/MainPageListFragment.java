package com.example.SpeakEasy;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.example.SpeakEasy.categoryFragments.UserFeedFragment;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphObject;
import com.facebook.model.OpenGraphAction;
import com.facebook.model.OpenGraphObject;
import com.facebook.widget.FacebookDialog;
import de.hdodenhof.circleimageview.CircleImageView;
import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainPageListFragment extends ListFragment implements SwipeRefreshLayout.OnRefreshListener {
    protected List<String> itemNames;
    protected String userId;
    protected static MySimpleArrayAdapter adapter;
    protected UiLifecycleHelper uiHelper;
    protected MaterialNavigationDrawer mActivity;
    protected FloatingActionButton fabButton;
    protected SwipeRefreshLayout swipeLayout;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        userId = mActivity.getSharedPreferences("fbInfo", Context.MODE_PRIVATE).getString("id", "");
        uiHelper = new UiLifecycleHelper(this.mActivity, null);
        uiHelper.onCreate(savedInstanceState);
        mActivity.setTitle(getFragmentTitle());

        new Thread(new Runnable() {
            public void run() {
                itemNames = SimpleDB.getFeedItemNames(userId);
                adapter = new MySimpleArrayAdapter(inflater.getContext(), itemNames);
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setListAdapter(adapter);

                    }
                });
            }
        }).start();

        // Add FAB
        fabButton = new FloatingActionButton.Builder(mActivity)
                .withDrawable(getResources().getDrawable(R.drawable.ic_action_edit))
                .withButtonColor(0xFF2196F3)
                .withGravity(Gravity.BOTTOM | Gravity.END)
                .withMargins(0, 0, 16, 16)
                .create();

        fabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCategories();
            }
        });

        final Object[] toggleInfo = mActivity.getToggleInfo();

        ActionBarDrawerToggle newToggle = new ActionBarDrawerToggle(
                mActivity,
                (DrawerLayout) toggleInfo[1],
                mActivity.getToolbar(),
                R.string.drawer_open,
                R.string.drawer_close) {

            @Override
            public void onDrawerSlide(View drawerView, float offset) {
                fabButton.setAlpha(1 - offset);
            }

            public void onDrawerClosed(View view) {
                fabButton.setEnabled(true);
                fabButton.setClickable(true);

                mActivity.invalidateOptionsMenu();
                toggleInfo[4] = false;
                mActivity.setSectionsTouch(!((Boolean) toggleInfo[4]));

                if (toggleInfo[3] != null)
                    ((DrawerLayout.DrawerListener) toggleInfo[3]).onDrawerClosed(view);

            }

            public void onDrawerOpened(View drawerView) {
                mActivity.invalidateOptionsMenu();
                //fabButton.setVisibility(View.GONE);
                fabButton.setClickable(false);
                fabButton.setEnabled(false);

                if (toggleInfo[3] != null)
                    ((DrawerLayout.DrawerListener) toggleInfo[3]).onDrawerOpened(drawerView);
            }
        };

        mActivity.setToggle(newToggle);
        mActivity.setDrawerListener(newToggle);

        return inflater.inflate(R.layout.main_listfragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupRefresh();
    }

    public void setupRefresh() {
        // Swipe to refresh listView
        swipeLayout = (SwipeRefreshLayout) getView().findViewById(R.id.main_listfrag);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorScheme(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        final ListView listView = getListView();
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                boolean enable = false;

                /**
                 * This enables us to force the layout to refresh only when the first item
                 * of the list is visible.
                 **/
                if (listView != null && listView.getChildCount() > 0) {
                    // check if the first item of the list is visible
                    boolean firstItemVisible = listView.getFirstVisiblePosition() == 0;
                    // check if the top of the first item is visible
                    boolean topOfFirstItemVisible = listView.getChildAt(0).getTop() == 0;
                    // enabling or disabling the refresh layout
                    enable = firstItemVisible && topOfFirstItemVisible;
                }
                swipeLayout.setEnabled(enable);
            }
        });
    }

    /**
     * Called when the listView is pulled down for the data to refresh
     */
    @Override
    public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    public void run() {
                        itemNames = SimpleDB.getFeedItemNames(userId);
                        adapter = new MySimpleArrayAdapter(mActivity, itemNames);
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setListAdapter(adapter);
                                swipeLayout.setRefreshing(false);
                            }
                        });
                    }
                }).start();
            }
        }, 0);
    }

    public String getFragmentTitle() {
        return "All Quotes";
    }

    @Override
    public void onResume() {
        super.onResume();
        mActivity.setTitle(getFragmentTitle());
        uiHelper.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (MaterialNavigationDrawer) activity;
    }

    public static void shareToFB(Activity activity, String quoteText, UiLifecycleHelper uiHelper) {
        if (FacebookDialog.canPresentOpenGraphActionDialog(activity.getApplicationContext(),
                FacebookDialog.OpenGraphActionDialogFeature.OG_ACTION_DIALOG)) {
            OpenGraphObject quote = OpenGraphObject.Factory.createForPost
                    (OpenGraphObject.class, "speakeasydevfest:post", "I loved this quote!",
                            "http://i.imgur.com/ec9p33P.jpg", null, "\"" + quoteText + "\"");
            OpenGraphAction action = GraphObject.Factory.create(OpenGraphAction.class);
            action.setProperty("quote", quote);
            action.setType("speakeasydevfest:love");
            FacebookDialog shareDialog = new FacebookDialog.OpenGraphActionDialogBuilder(activity, action, "quote")
                    .build();
            uiHelper.trackPendingDialogCall(shareDialog.present());
        } else {
            Toast.makeText(activity, "Facebook not available", Toast.LENGTH_SHORT).show();
        }
    }

    // DialogFragment to select 0 or more categories
    public void selectCategories() {
        DialogFragment newFragment = new CategoryChooserFragment();
        FragmentManager supportFragmentManager = mActivity.getSupportFragmentManager();
        newFragment.show(supportFragmentManager, "categories");
    }

    protected static class ViewHolder {
        CircleImageView profilePicture;
        TextView authorFbName;
        TextView quoteText;
        TextView quoteAuthor;
        ImageView fbShare;
        ImageView follow;
        ImageView comment;
        ImageView favorite;
        TextView numFavs;
        ImageView heart;
        String postID;
        String posterId;
        String timestamp;
    }

    protected class MySimpleArrayAdapter extends ArrayAdapter<String> {
        private final Context context;
        private List<String> quoteItemNames;

        public MySimpleArrayAdapter(Context context, List<String> values) {
            super(context, R.layout.main_item_view, values);
            this.context = context;
            this.quoteItemNames = values;
            notifyDataSetChanged();
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
        }

        @Override
        public void add(String object) {
            if (quoteItemNames == null) {
                quoteItemNames = new ArrayList<String>();
                quoteItemNames.add(object);
            } else {
                quoteItemNames.add(0, object);
            }
        }

        @Override
        public void remove(String object) {
            super.remove(object);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder viewHolder;
            if (convertView == null) {
                // inflate the layout
                LayoutInflater inflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.main_item_view, parent, false);

                // set up the ViewHolder
                viewHolder = new ViewHolder();
                viewHolder.profilePicture = (CircleImageView) convertView.findViewById(R.id.profile_image);
                viewHolder.fbShare = (ImageView) convertView.findViewById(R.id.shareIcon);
                viewHolder.follow = (ImageView) convertView.findViewById(R.id.mainFollow);
                viewHolder.comment = (ImageView) convertView.findViewById(R.id.comment);
                viewHolder.favorite = (ImageView) convertView.findViewById(R.id.heart);
                viewHolder.numFavs = (TextView) convertView.findViewById(R.id.numFavorites);
                viewHolder.heart = (ImageView) convertView.findViewById(R.id.heart);
                convertView.setTag(viewHolder);
            } else {
                // we've just avoided calling findViewById() on resource every time
                // just use the viewHolder
                viewHolder = (ViewHolder) convertView.getTag();
            }

            HashMap<String, String> attrMap = SimpleDB.getAttributesForItem("Quotes", quoteItemNames.get(position));
            viewHolder.posterId = attrMap.get("userId");

            // Set profile picture of the poster
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final Bitmap profilePic;
                        String profileUrlString = "https://graph.facebook.com/" + viewHolder.posterId + "/picture?type=large";
                        URL imgUrl = new URL(profileUrlString);
                        InputStream in = (InputStream) imgUrl.getContent();
                        profilePic = BitmapFactory.decodeStream(in);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                viewHolder.profilePicture.setImageBitmap(profilePic);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            viewHolder.authorFbName = (TextView) convertView.findViewById(R.id.mainFBName);
            viewHolder.authorFbName.setText(attrMap.get("fbName"));
            viewHolder.timestamp = attrMap.get("timestamp");
            viewHolder.postID = viewHolder.authorFbName.getText().toString().replace(" ", "") + viewHolder.timestamp;
            viewHolder.posterId = attrMap.get("userId");
            viewHolder.quoteText = (TextView) convertView.findViewById(R.id.mainItemText);
            viewHolder.quoteAuthor = (TextView) convertView.findViewById(R.id.mainItemAuthor);
            viewHolder.quoteAuthor.setText("- " + attrMap.get("author"));
            viewHolder.quoteText.setText("\"" + attrMap.get("quoteText") + "\"  ");
            final SharedPreferences prefs = mActivity.getSharedPreferences("fbInfo", Context.MODE_PRIVATE);
            final String username = prefs.getString("name", "");
            final String userId = prefs.getString("id", "");

            final ColorFilter filter = new LightingColorFilter(Color.parseColor("#ff5c5d54"), Color.parseColor("#ff5c5d54"));

            Drawable shareIcon = mActivity.getResources().getDrawable(R.drawable.fbshare);
            Drawable followIcon = mActivity.getResources().getDrawable(R.drawable.ic_action_add);
            final Drawable heartIcon = mActivity.getResources().getDrawable(R.drawable.ic_action_heart);
            Drawable commentIcon = mActivity.getResources().getDrawable(R.drawable.ic_action_comment);

            shareIcon.setColorFilter(filter);
            viewHolder.fbShare.setImageDrawable(shareIcon);
            followIcon.setColorFilter(filter);
            viewHolder.follow.setImageDrawable(followIcon);
            heartIcon.setColorFilter(filter);
            viewHolder.heart.setImageDrawable(heartIcon);
            commentIcon.setColorFilter(filter);
            viewHolder.comment.setImageDrawable(commentIcon);

            if (viewHolder.authorFbName.getText().toString().equals(username)) {
                viewHolder.follow.setVisibility(View.GONE);
            }

            final int[] numFavs = new int[1];
            final boolean[] isFav = new boolean[1];
            new Thread(new Runnable() {
                public void run() {
                    numFavs[0] = SimpleDB.favCount(viewHolder.postID);
                    isFav[0] = SimpleDB.isFavoritedByUser(viewHolder.postID, userId);
                    final boolean isFollowed = SimpleDB.isFollowedByUser(viewHolder.posterId, userId);

                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (numFavs[0] == 0 && viewHolder.numFavs.getVisibility() == View.VISIBLE) {
                                viewHolder.numFavs.setVisibility(View.GONE);
                            } else if (numFavs[0] == 1) {
                                viewHolder.numFavs.setText("1 Favorite");
                                viewHolder.numFavs.setVisibility(View.VISIBLE);
                            } else if (viewHolder.numFavs.getVisibility() == View.VISIBLE) {
                                viewHolder.numFavs.setText(numFavs[0] + " Favorites");
                            }

                            if (isFav[0]) {
                                ColorFilter redFilter = new LightingColorFilter(Color.RED, Color.RED);
                                heartIcon.setColorFilter(redFilter);
                                viewHolder.heart.setImageDrawable(heartIcon);
                            } else if (!isFav[0]) {
                                heartIcon.setColorFilter(filter);
                                viewHolder.heart.setImageDrawable(heartIcon);
                            }

                            if (isFollowed || viewHolder.posterId.equals(userId)) {
                                viewHolder.follow.setVisibility(View.GONE);
                            } else {
                                viewHolder.follow.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                }
            }).start();

            viewHolder.favorite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final HashMap<String, String> newFavAttr = new HashMap<String, String>();
                    if (viewHolder.posterId.equals(userId)) {
                        Toast.makeText(mActivity, "Stop trying to like your own post!", Toast.LENGTH_SHORT).show();
                    } else if (isFav[0]) {
                        new Thread(new Runnable() {
                            public void run() {
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        heartIcon.setColorFilter(filter);
                                        viewHolder.heart.setImageDrawable(heartIcon);
                                        if (numFavs[0] == 1) {
                                            viewHolder.numFavs.setVisibility(View.GONE);
                                        } else if (numFavs[0] == 2) {
                                            viewHolder.numFavs.setText("1 Favorite");
                                        } else {
                                            viewHolder.numFavs.setText(numFavs[0] - 1 + " Favorites");
                                        }
                                    }
                                });
                                SimpleDB.deleteItem("Favorites", viewHolder.postID + "_likedBy_" + userId);
                                newFavAttr.put("favorites", "" + (numFavs[0] - 1));
                                SimpleDB.updateAttributesForItem("Quotes", viewHolder.postID, newFavAttr);
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                            }
                        }).start();
                    } else {
                        new Thread(new Runnable() {
                            public void run() {
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ColorFilter redFilter = new LightingColorFilter(Color.RED, Color.RED);
                                        heartIcon.setColorFilter(redFilter);
                                        viewHolder.heart.setImageDrawable(heartIcon);
                                        if (numFavs[0] == 0) {
                                            viewHolder.numFavs.setVisibility(View.VISIBLE);
                                            viewHolder.numFavs.setText("1 Favorite");
                                        } else {
                                            viewHolder.numFavs.setText(numFavs[0] + 1 + " Favorites");
                                        }
                                    }
                                });
                                SimpleDB.addToFavoriteTable(viewHolder.postID, userId);
                                newFavAttr.put("favorites", "" + (numFavs[0] + 1));
                                SimpleDB.updateAttributesForItem("Quotes", viewHolder.postID, newFavAttr);
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                            }
                        }).start();
                    }
                }
            });

            viewHolder.fbShare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareToFB(mActivity, viewHolder.quoteText.getText().toString(), uiHelper);
                }
            });

            viewHolder.follow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        public void run() {
                            SimpleDB.addToFollowingTable(viewHolder.posterId, userId);
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }).start();
                }
            });

            viewHolder.authorFbName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    android.support.v4.app.FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
                    android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    Bundle bundle = new Bundle();
                    bundle.putString("userId", viewHolder.posterId);
                    bundle.putString("username", viewHolder.authorFbName.getText().toString());
                    Fragment fragment = new UserFeedFragment();
                    fragment.setArguments(bundle);
                    fragmentTransaction.replace(it.neokree.materialnavigationdrawer.R.id.frame_container, fragment);
                    fragmentTransaction.addToBackStack(null).commit();
                }
            });
            return convertView;
        }
    }
}