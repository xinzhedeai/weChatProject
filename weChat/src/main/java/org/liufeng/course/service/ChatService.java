package org.liufeng.course.service;

import java.io.File;
import java.util.List;
import java.util.Random;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.liufeng.course.pojo.Knowledge;
import org.liufeng.course.util.MySQLUtil;
import org.wltea.analyzer.lucene.IKAnalyzer;

/**
 * ���������
 * 
 * @author liufeng
 * @date 2013-12-01
 */
public class ChatService {
	/**
	 * �õ������洢Ŀ¼
	 * 
	 * @return WEB-INF/classes/index/
	 */
	public static String getIndexDir() {
		// �õ�.class�ļ�����·����WEB-INF/classes/��
		String classpath = ChatService.class.getResource("/").getPath();
		// ��classpath�е�%20�滻Ϊ�ո�
		classpath = classpath.replaceAll("%20", " ");
		// �����洢λ�ã�WEB-INF/classes/index/
		return classpath + "index/";
	}

	/**
	 * ��������
	 */
	public static void createIndex() {
		// ȡ���ʴ�֪ʶ���е����м�¼
		List<Knowledge> knowledgeList = MySQLUtil.findAllKnowledge();
		Directory directory = null;
		IndexWriter indexWriter = null;
		try {
			directory = FSDirectory.open(new File(getIndexDir()));
			IndexWriterConfig iwConfig = new IndexWriterConfig(Version.LUCENE_46, new IKAnalyzer(true));
			indexWriter = new IndexWriter(directory, iwConfig);
			Document doc = null;
			// �����ʴ�֪ʶ�ⴴ������
			for (Knowledge knowledge : knowledgeList) {
				doc = new Document();
				// ��question���зִʴ洢
				doc.add(new TextField("question", knowledge.getQuestion(), Store.YES));
				// ��id��answer��category���ִʴ洢
				doc.add(new IntField("id", knowledge.getId(), Store.YES));
				doc.add(new StringField("answer", knowledge.getAnswer(), Store.YES));
				doc.add(new IntField("category", knowledge.getCategory(), Store.YES));
				indexWriter.addDocument(doc);
			}
			indexWriter.close();
			directory.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * �������ļ��и������������
	 * 
	 * @param content
	 * @return Knowledge
	 */
	@SuppressWarnings("deprecation")
	private static Knowledge searchIndex(String content) {
		Knowledge knowledge = null;
		try {
			Directory directory = FSDirectory.open(new File(getIndexDir()));
			IndexReader reader = IndexReader.open(directory);
			IndexSearcher searcher = new IndexSearcher(reader);
			// ʹ�ò�ѯ����������Query
			QueryParser questParser = new QueryParser(Version.LUCENE_46, "question", new IKAnalyzer(true));
			Query query = questParser.parse(QueryParser.escape(content));
			// �����÷���ߵ��ĵ�
			TopDocs topDocs = searcher.search(query, 1);
			if (topDocs.totalHits > 0) {
				knowledge = new Knowledge();
				ScoreDoc[] scoreDoc = topDocs.scoreDocs;
				for (ScoreDoc sd : scoreDoc) {
					Document doc = searcher.doc(sd.doc);
					knowledge.setId(doc.getField("id").numericValue().intValue());
					knowledge.setQuestion(doc.get("question"));
					knowledge.setAnswer(doc.get("answer"));
					knowledge.setCategory(doc.getField("category").numericValue().intValue());
				}
			}
			reader.close();
			directory.close();
		} catch (Exception e) {
			knowledge = null;
			e.printStackTrace();
		}
		return knowledge;
	}

	/**
	 * ���췽��������question����answer��
	 * 
	 * @param openId �û���OpenID
	 * @param createTime ��Ϣ����ʱ��
	 * @param question �û����е�����
	 * @return answer
	 */
	public static String chat(String openId, String createTime, String question) {
		String answer = null;
		int chatCategory = 0;
		Knowledge knowledge = searchIndex(question);
		// �ҵ�ƥ����
		if (null != knowledge) {
			// Ц��
			if (2 == knowledge.getCategory()) {
				answer = MySQLUtil.getJoke();
				chatCategory = 2;
			}
			// ������
			else if (3 == knowledge.getCategory()) {
				// �ж���һ�ε��������
				int category = MySQLUtil.getLastCategory(openId);
				// �����Ц�������μ����ظ�Ц�����û�
				if (2 == category) {
					answer = MySQLUtil.getJoke();
					chatCategory = 2;
				} else {
					answer = knowledge.getAnswer();
					chatCategory = knowledge.getCategory();
				}
			}
			// ��ͨ�Ի�
			else {
				answer = knowledge.getAnswer();
				// �����Ϊ�գ�����֪ʶid���ʴ�֪ʶ�ֱ��������ȡһ��
				if ("".equals(answer))
					answer = MySQLUtil.getKnowledSub(knowledge.getId());
				chatCategory = 1;
			}
		}
		// δ�ҵ�ƥ����
		else {
			answer = getDefaultAnswer();
			chatCategory = 0;
		}
		// ���������¼
		MySQLUtil.saveChatLog(openId, createTime, question, answer, chatCategory);
		return answer;
	}

	/**
	 * �����ȡһ��Ĭ�ϵĴ�
	 * 
	 * @return
	 */
	private static String getDefaultAnswer() {
		String []answer = {
			"Ҫ�������ĵ��ģ�",
			"�����㵽����˵ʲô�أ�",
			"û��������˵�ģ��ܷ񻻸�˵����",
			"��Ȼ�����������˼������ȴ������ȥ����",
			"������һͷ��ˮ�����µ�֪ʶ����Ԩ��ѽ��Ĥ��~",
			"��������������˵ʲô��Ҫ���㻻�ֱ�﷽ʽ��Σ�",
			"������Сѧ������������ʦ�̵ģ���������е�����Ŷ",
			"������仯̫�죬�����Ҳ����вţ�Ϊ����˵���Ҳ����ף�"
		};
		return answer[getRandomNumber(answer.length)];
	}

	/**
	 * ������� 0~length-1 ֮���ĳ��ֵ
	 * 
	 * @return int
	 */
	private static int getRandomNumber(int length) {
		Random random = new Random();
		return random.nextInt(length);
	}
}
