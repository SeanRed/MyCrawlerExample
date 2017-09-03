package crawler.example.integration;

import com.github.abola.crawler.CrawlerPack;
import com.mashape.unirest.http.Unirest;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Created by Abola Lee on 2016/7/10.
 */
public class FBElasticImport {

    static String elasticHost = "dyn.gibar.co" ;
    static String elasticPort = "9200" ;
    static String elasticIndex = "YourGitHubID";
    static String elasticIndexType = "data";
    static String pageName = "JudgeAd";
    // 2017-09-02
    static long start = 1504363907;
    // 往前抓抓取日期數
    static int days = 10;
    // 每日抓取文章上限 (上限1000)
    static int maxPosts = 10;
    static String access_token = "EAACEdEose0cBAK9hBiFF137BRoevmmZAHJvNVsfTM9DfxDQt9ZBigLj1plmrkYZA5R8GrthbDFDxGWDj2zVsckZBoyCGHVZCOOjm8JtagFzrM4dw46eaprF1KBzisC7j7Qf4v1JtNKW6q4lDAuHc0ItUNKjf8Uc2kBibcUTXGK3HBk1XkrEs3CiyrHCZCWZAocZD";

    public static void main(String[] args) {

        for (long datatime = start ; datatime > start-86400*days ;datatime-=86400) {
            String uri =
                    "https://graph.facebook.com/v2.6"
                            + "/"+pageName +"/posts?fields=message,comments.limit(0).summary(true),likes.limit(0).summary(true),created_time&since="+(datatime-3600*8)+"&until="+datatime+"&limit="+maxPosts
                            + "&access_token="+access_token;



            try {

                Elements elems =
                        CrawlerPack.start()
                                .getFromJson(uri)
                                .select("data:has(created_time)");

                System.out.println(elems.size());
                // 遂筆處理
                for (Element data : elems) {


                    String created_time = data.select("created_time").text();
                    String id = data.select("id").text();
                    String message = data.select("message").text();
                    String likes = data.select("likes > summary > total_count").text();
                    String comments = data.select("comments > summary > total_count").text();

                    // Elasticsearch data format

                    String elasticJson = "{" +
                            "\"created_time\":\"" + created_time + "\"" +
                            ",\"message\":\"" + message.replaceAll("\"","'") + "\"" +
                            ",\"likes\":" + likes +
                            ",\"id\":\"" + id + "\"" +
                            ",\"pagename\":\"" + pageName + "\"" +
                            ",\"comments\":" + comments +

                            "}";
                    // debug 看看資料會如何呈現
                    //System.out.println(elasticJson);
                    System.out.println(
                            // curl -XPOST http://localhost:9200/pm25/data -d '{...}'
                            sendPost("http://" + elasticHost + ":" + elasticPort
                                            + "/" + elasticIndex + "/" + elasticIndexType
                                    , elasticJson));
                }
            }catch(Exception e){/*不良示範*/}

        }
    }



    /***
     * NVL
     * if arg0 isNull then return arg1
     */
    static public <T> T nvl(T arg0, T arg1) {
        return (arg0 == null)?arg1:arg0;
    }

    static String sendPost(String url, String body){
        try{
            return Unirest.post(url)
                    .header("content-type", "text/plain")
                    .header("cache-control", "no-cache")
                    .body(body)
                    .asString().getBody();

        }catch(Exception e){return "Error:" + e.getMessage();}
    }
}
