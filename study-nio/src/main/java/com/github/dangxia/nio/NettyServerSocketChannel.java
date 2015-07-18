package com.github.dangxia.nio;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettyServerSocketChannel {
	
	
	public NettyServerSocketChannel() {
		// TODO Auto-generated constructor stub
	}

	public void run(int port) throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap(); // (2)
			b.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class) // (3)
					.childHandler(new ChannelInitializer<SocketChannel>() { // (4)
								@Override
								public void initChannel(SocketChannel ch)
										throws Exception {
									ch.pipeline().addLast(
											new DiscardServerHandler());
								}
							}).option(ChannelOption.SO_BACKLOG, 128) // (5)
					.childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

			// Bind and start to accept incoming connections.
			ChannelFuture f = b.bind(port).sync(); // (7)

			// Wait until the server socket is closed.
			// In this example, this does not happen, but you can do that to
			// gracefully
			// shut down your server.
			f.channel().closeFuture().sync();
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}

	}

	public static void main(String[] args) throws Exception {
		new NettyServerSocketChannel().run(9797);
	}

	public class DiscardServerHandler extends ChannelInboundHandlerAdapter { // (1)
		public void channelRead(ChannelHandlerContext ctx, Object msg) { // (2)
			ctx.write(msg); // (1)
			ctx.flush(); // (2)
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
			// Close the connection when an exception is raised.
			cause.printStackTrace();
			ctx.close();
		}
	}

}
