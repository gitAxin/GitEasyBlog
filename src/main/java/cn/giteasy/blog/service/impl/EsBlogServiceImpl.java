package cn.giteasy.blog.service.impl;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cn.giteasy.blog.entity.User;
import cn.giteasy.blog.entity.es.EsBlog;
import cn.giteasy.blog.repository.UserRepository;
import cn.giteasy.blog.repository.es.EsBlogRepository;
import cn.giteasy.blog.service.EsBlogService;
import cn.giteasy.blog.service.UserService;
import cn.giteasy.blog.vo.TagVO;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.search.SearchParseException;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ResultsExtractor;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


/**
 * EsBlog 服务.
 * 
 * @since 1.0.0 2017年4月12日
 * @author <a href="https://waylau.com">Way Lau</a>
 */
@Service
public class EsBlogServiceImpl implements EsBlogService {
	@Autowired
	private EsBlogRepository esBlogRepository;
	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;
	@Autowired
	private UserService userService;
	@Autowired
	private UserRepository userRepository;
	
	private static final Pageable TOP_5_PAGEABLE = PageRequest.of(0, 5);
	private static final String EMPTY_KEYWORD = "";


	@Override
	public void removeEsBlog(String id) {
		esBlogRepository.deleteById(id);
	}



	@Override
	public EsBlog updateEsBlog(EsBlog esBlog) {
		return esBlogRepository.save(esBlog);
	}



	@Override
	public EsBlog getEsBlogByBlogId(Long blogId) {
		return esBlogRepository.findByBlogId(blogId);
	}



	@Override
	public Page<EsBlog> listNewestEsBlogs(String keyword, Pageable pageable) throws SearchParseException {
		Page<EsBlog> pages = null;
		Sort sort = Sort.by(Sort.Direction.DESC,"createTime");
		pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

		if (pageable.getSort() == null) {

		}
 
		pages = esBlogRepository.findDistinctEsBlogByTitleContainingOrSummaryContainingOrContentContainingOrTagsContaining(keyword,keyword,keyword,keyword, pageable);

		System.out.println(pages.getContent());

		return pages;
	}



	@Override
	public Page<EsBlog> listHotestEsBlogs(String keyword, Pageable pageable) throws SearchParseException{
 
		Sort sort = Sort.by(Sort.Direction.DESC,"readSize","commentSize","voteSize","createTime");
		pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
 
		return esBlogRepository.findDistinctEsBlogByTitleContainingOrSummaryContainingOrContentContainingOrTagsContaining(keyword, keyword, keyword, keyword, pageable);
	}

	@Override
	public Page<EsBlog> listEsBlogs(Pageable pageable) {
		return esBlogRepository.findAll(pageable);
	}
 
 
	/**
	 * 最新前5
	 * @return
	 */
	@Override
	public List<EsBlog> listTop5NewestEsBlogs() {
		Page<EsBlog> page = this.listNewestEsBlogs(EMPTY_KEYWORD, TOP_5_PAGEABLE);
		System.out.println(page.getContent());
		return page.getContent();
	}
	
	/**
	 * 最热前5
	 * @return
	 */
	@Override
	public List<EsBlog> listTop5HotestEsBlogs() {
		Page<EsBlog> page = this.listHotestEsBlogs(EMPTY_KEYWORD, TOP_5_PAGEABLE);
		return page.getContent();
	}

	@Override
	public List<TagVO> listTop30Tags() {
 
		List<TagVO> list = new ArrayList<>();
		// given
		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(matchAllQuery())
				.withSearchType(SearchType.QUERY_THEN_FETCH)
				.withIndices("blog").withTypes("blog")
				.addAggregation(terms("tags").field("tags").order(BucketOrder.count(false)).size(30))
				.build();
		// when
		Aggregations aggregations = elasticsearchTemplate.query(searchQuery, new ResultsExtractor<Aggregations>() {
			@Override
			public Aggregations extract(SearchResponse response) {
				return response.getAggregations();
			}
		});
		
		StringTerms modelTerms =  (StringTerms)aggregations.asMap().get("tags"); 
	        
        Iterator<StringTerms.Bucket> modelBucketIt = modelTerms.getBuckets().iterator();
        while (modelBucketIt.hasNext()) {
            Bucket actiontypeBucket = modelBucketIt.next();
 
            list.add(new TagVO(actiontypeBucket.getKey().toString(),
                    actiontypeBucket.getDocCount()));
        }
		return list;
	}
	
	@Override
	public List<User> listTop12Users() {
 
		List<String> usernamelist = new ArrayList<>();
		// given
		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(matchAllQuery())
				.withSearchType(SearchType.QUERY_THEN_FETCH)
				.withIndices("blog").withTypes("blog")
				.addAggregation(terms("users").field("username").order(BucketOrder.count(false)).size(12))
				.build();
		// when
		Aggregations aggregations = elasticsearchTemplate.query(searchQuery, new ResultsExtractor<Aggregations>() {
			@Override
			public Aggregations extract(SearchResponse response) {
				return response.getAggregations();
			}
		});
		
		StringTerms modelTerms =  (StringTerms)aggregations.asMap().get("users");

		Iterator<StringTerms.Bucket> modelBucketIt = modelTerms.getBuckets().iterator();
        while (modelBucketIt.hasNext()) {
            Bucket actiontypeBucket = modelBucketIt.next();
            String username = actiontypeBucket.getKey().toString();
            usernamelist.add(username);
        }
        List<User> list = userService.listUsersByUsernames(usernamelist);
        
		return list;
	}


	@Override
	public void addAvatar(List<EsBlog> list) {
		if(!CollectionUtils.isEmpty(list)){
			for (EsBlog esBlog : list) {
				String username = esBlog.getUsername();
				User user = userRepository.findByUsername(username);
				esBlog.setAvatar(user.getAvatar());
			}
		}

	}
}
