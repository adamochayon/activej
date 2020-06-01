import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import io.activej.bytebuf.ByteBuf;
import io.activej.bytebuf.util.ByteBufWriter;
import io.activej.common.collection.Either;
import io.activej.di.annotation.Provides;
import io.activej.http.AsyncServlet;
import io.activej.http.AsyncServletDecorator;
import io.activej.http.HttpResponse;
import io.activej.http.RoutingServlet;
import io.activej.http.decoder.DecodeErrors;
import io.activej.http.decoder.Decoder;
import io.activej.launcher.Launcher;
import io.activej.launchers.http.HttpServerLauncher;

import java.util.Map;

import static io.activej.common.collection.CollectionUtils.map;
import static io.activej.http.HttpMethod.POST;
import static io.activej.http.decoder.Decoders.ofPost;

//[START REGION_1]
public final class HttpDecoderExample extends HttpServerLauncher {
	private final static String SEPARATOR = "-";

	private final static Decoder<Address> ADDRESS_DECODER = Decoder.of(Address::new,
			ofPost("title", "")
					.validate(param -> !param.isEmpty(), "Title cannot be empty")
	);

	private final static Decoder<Contact> CONTACT_DECODER = Decoder.of(Contact::new,
			ofPost("name")
					.validate(name -> !name.isEmpty(), "Name cannot be empty"),
			ofPost("age")
					.map(Integer::valueOf, "Cannot parse age")
					.validate(age -> age >= 18, "Age must be greater than 18"),
			ADDRESS_DECODER.withId("contact-address")
	);
	//[END REGION_1]

	//[START REGION_5]
	private static ByteBuf applyTemplate(Mustache mustache, Map<String, Object> scopes) {
		ByteBufWriter writer = new ByteBufWriter();
		mustache.execute(writer, scopes);
		return writer.getBuf();
	}
	//[END REGION_5]

	//[START REGION_6]
	@Provides
	ContactDAO dao() {
		return new ContactDAOImpl();
	}
	//[END REGION_6]

	//[START REGION_2]
	@Provides
	AsyncServlet mainServlet(ContactDAO contactDAO) {
		Mustache contactListView = new DefaultMustacheFactory().compile("static/contactList.html");
		return RoutingServlet.create()
				.map("/", request ->
						HttpResponse.ok200()
								.withBody(applyTemplate(contactListView, map("contacts", contactDAO.list()))))
				.map(POST, "/add", AsyncServletDecorator.loadBody()
						.serve(request -> {
							//[START REGION_3]
							Either<Contact, DecodeErrors> decodedUser = CONTACT_DECODER.decode(request);
							//[END REGION_3]
							if (decodedUser.isLeft()) {
								contactDAO.add(decodedUser.getLeft());
							}
							Map<String, Object> scopes = map("contacts", contactDAO.list());
							if (decodedUser.isRight()) {
								scopes.put("errors", decodedUser.getRight().toMap(SEPARATOR));
							}
							return HttpResponse.ok200()
									.withBody(applyTemplate(contactListView, scopes));
						}));
	}
	//[END REGION_2]

	//[START REGION_4]
	public static void main(String[] args) throws Exception {
		Launcher launcher = new HttpDecoderExample();
		launcher.launch(args);
	}
	//[END REGION_4]
}