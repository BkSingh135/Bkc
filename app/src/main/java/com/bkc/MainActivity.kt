package com.bkc

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.FrameLayout
import android.widget.TextClock
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bkc.carouselLayoutManager.CarouselLayoutManager
import com.bkc.carouselLayoutManager.CarouselZoomPostLayoutListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.String


class MainActivity : BaseActivity() {


    var miniRv : RecyclerView ?= null
    var miniAdapter : MiniAdapter ?= null
    var viewPager : ViewPager2?= null
    var viewPagerAdapter : ViewPagerAdapter ?= null
    var list : ArrayList<BanModel> ?= null
    private var handler: Handler? = null
    var runnable: Runnable? = null
    private val speedScroll = 7000
    var carouselLayoutManager : CarouselLayoutManager ?= null
    private val timeLaps = true
    private var chID : AppCompatTextView ?= null
    private var marqueeTv : AppCompatTextView ?= null
    private var clock : TextClock ?= null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        hideStatusBarWithBottomNavWithShadowNav()
        initViews()
    }

    @SuppressLint("PrivateResource")
    private fun initViews() {
       // val carouselLayoutManager: CarouselLayoutManager = containerFragment.getmRvChannelList().getLayoutManager() as CarouselLayoutManager
        clock = findViewById(R.id.textClock)
        clock!!.format12Hour = "hh:mm a"
        viewPager = findViewById(R.id.viewPager)
        viewPager!!.orientation = ViewPager2.ORIENTATION_VERTICAL

        chID = findViewById(R.id.chID)
        marqueeTv = findViewById(R.id.marqueeTv)
        marqueeTv!!.text = getString(R.string.dummy_text)
        marqueeTv!!.isSelected = true
         miniRv = findViewById(R.id.miniRv)
        list = ArrayList()
        getSuperHeroes()
        getTickerText()

       // viewPagerAdapter = ViewPagerAdapter(/*data,*/this)
        viewPager?.adapter = viewPagerAdapter


    }

    private fun getTickerText() {
        val call : Call<Ticker> = RetrofitClient.instance.apiService.getTicker()
        call.enqueue(object  : Callback<Ticker>{
            override fun onResponse(
                call: Call<Ticker>,
                response: Response<Ticker>
            ) {
                if (!response.isSuccessful)
                {
                    return
                }
                val tick : Ticker = response.body()!!
                Log.e("text", "onResponse: ${tick.text}")
                    marqueeTv?.text = tick.text
            }

            override fun onFailure(call: Call<Ticker>, t: Throwable) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun getSuperHeroes() {
        val call: Call<ArrayList<BanModel>> = RetrofitClient.instance.apiService.poster("hi")
        call.enqueue(object  : Callback<ArrayList<BanModel>> {
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(
                call: Call<ArrayList<BanModel>>,
                response: Response<ArrayList<BanModel>>
            ) {
                if (!response.isSuccessful)
                {
                    return
                }
                list = response.body()
                try
                {
                    setViewPagerAdapter(viewPager)
                    if (list != null && list!!.size > 0)
                    {
                        setChannel(miniRv, list!!)
                    }
                }
                catch (e : Exception)
                {
                    e.printStackTrace()
                    Toast.makeText(applicationContext,"Error",Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ArrayList<BanModel>>, t: Throwable) {
               Toast.makeText(applicationContext,"Error",Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun setViewPagerAdapter(viewPager: ViewPager2?) {
       // viewPager.setPagingEnabled(false)
        viewPager!!.offscreenPageLimit = 3

        viewPagerAdapter =
            ViewPagerAdapter(list!!,applicationContext)

        viewPager.adapter = viewPagerAdapter
        viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int)
            {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                //if (list != null)

               /* list =*/ viewPager.currentItem = position

            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
            }
        })

    }

    private fun setChannel(miniRv: RecyclerView?, lists: ArrayList<BanModel>) {
        if (miniRv!!.adapter != null) {
            (miniRv.adapter as MiniAdapter).addAll(lists)
        } else {
            val layoutManager = CarouselLayoutManager(CarouselLayoutManager.VERTICAL, false)
            layoutManager.setPostLayoutListener(CarouselZoomPostLayoutListener())
            if (lists.size > 5) {
                layoutManager.maxVisibleItems = 5
            } else if (lists.size > 0) {
                layoutManager.maxVisibleItems = lists.size
            }
            miniAdapter = MiniAdapter(applicationContext,lists,object :
                MiniAdapter.onClickInterface {
                override fun OnClick(position: Int, miniModel: BanModel) {
                    handler?.removeCallbacks(runnable!!)
                    handler?.postDelayed(runnable!!, speedScroll.toLong())
                    loadData(miniModel, viewPager, position)
                    miniRv.smoothScrollToPosition(position)

                }
            })

            miniRv.layoutManager = layoutManager
            miniRv.itemAnimator = DefaultItemAnimator()
            miniRv.adapter = miniAdapter
        }
       // if (list.size > 0) loadData(channels.get(0), mVpPager, 0)
        handler = Handler()
        runnable = object : Runnable {
            var flag = true
            override fun run() {
                 carouselLayoutManager = miniRv.layoutManager as CarouselLayoutManager?
                // carouselLayoutManager  =(CarouselLayoutManager)miniRv.layoutManager
                if (carouselLayoutManager != null)
                {
                    var count = carouselLayoutManager!!.centerItemPosition
                    if (timeLaps
                        && miniRv.adapter != null && count < miniRv.adapter!!
                            .itemCount && miniRv.adapter!!.itemCount > 1
                    ) {
                        if (count == miniRv.adapter!!.itemCount - 1) {
                            flag = false
                        } else if (count == 0) {
                            flag = true
                        }
                        if (flag) count++ else count = 0
                        miniRv.scrollToPosition(count)
                        loadData(
                            (miniRv.adapter as MiniAdapter?)!!.getChannel(count),
                            viewPager,
                            count
                        )
                        handler!!.postDelayed(this, speedScroll.toLong())
                    }
                }
            }
        }
        handler!!.postDelayed(runnable as Runnable, speedScroll.toLong())

    }

    @SuppressLint("StringFormatInvalid")
    private fun loadData(list: BanModel, viewPager: ViewPager2?, position: Int) {
         viewPagerAdapter =
            viewPager!!.adapter as ViewPagerAdapter
        viewPager.currentItem = position
        chID!!.text = String.format(getString(R.string.channel_identifer), position)
//        channelIdenfier.setText("Ch-"+String.valueOf(position));

      //  handler?.removeCallbacks(runnable!!)
       // handler?.postDelayed(runnable!!, speedScroll.toLong())



    }

    private fun createAutoScroll() {}

    private fun setDataInRecyclerView() {
        miniRv?.layoutManager = LinearLayoutManager(this)
        miniAdapter = MiniAdapter(applicationContext,list!!,object : MiniAdapter.onClickInterface{
            override fun OnClick(position: Int, miniModel: BanModel)
            {

            }
        })
        miniRv?.adapter =  miniAdapter
    }
    /* override fun onBackPressed() {
         if (doubleBackToExitPressedOnce) {
 //            super.onBackPressed();
 //            return;
             finish()
         }
         doubleBackToExitPressedOnce = true
         Toast.makeText(this,  "Press again to exit", Toast.LENGTH_SHORT).show()
         Handler().postDelayed(
             { doubleBackToExitPressedOnce = false }, 2000
         )
     }*/
}





















